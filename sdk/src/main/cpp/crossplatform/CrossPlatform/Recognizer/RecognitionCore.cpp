//
//  RecognitionCore.cpp
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 12/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#include <thread>
#include <unistd.h>

#include "RecognitionCore.h"
#include "IServiceContainer.h"
#include "IFrameStorage.h"
#include "IEdgesDetector.h"
#include "IRecognitionResult.h"
#include "IDateRecognizer.h"
#include "INumberRecognizer.h"
#include "ITorchDelegate.h"
#include "ITorchManager.h"
#include "INameRecognizer.h"

#include "Utils.h"

#define kDateRecognitionAttempts  5

static int dateRecognitionAttemptsCount = 0;

bool IRecognitionCore::GetInstance(shared_ptr<IRecognitionCore> &recognitionCore,
                                   const shared_ptr<IRecognitionCoreDelegate>& recognitionDelegate,
                                   const shared_ptr<ITorchDelegate>& torchDelegate)
{
    recognitionCore = make_shared<CRecognitionCore>(recognitionDelegate, torchDelegate);
    return recognitionCore != 0;
}

CRecognitionCore::CRecognitionCore(const shared_ptr<IRecognitionCoreDelegate>& delegate, const shared_ptr<ITorchDelegate>& torchDelegate) : _delegate(delegate), _orientation(PayCardsRecognizerOrientationPortrait), _mode(PayCardsRecognizerModeNone), _deployed(false)
{
    _isIdle.store(false);
    _isBusy = false;

    IServiceContainerFactory::CreateServiceContainer(_serviceContainerPtr);
    
    _serviceContainerPtr->Initialize();
    _frameStorage = _serviceContainerPtr->resolve<IFrameStorage>();
    _edgesDetector = _serviceContainerPtr->resolve<IEdgesDetector>();
    _recognitionResult = _serviceContainerPtr->resolve<IRecognitionResult>();
    _torchManager = _serviceContainerPtr->resolve<ITorchManager>();
    
    if(auto torchManager = _torchManager.lock()) {
        torchManager->SetDelegate(torchDelegate);
    }
}

CRecognitionCore::~CRecognitionCore()
{
    _isIdle.store(false);
    _edges.clear();
    _currentFrame.release();
}

void CRecognitionCore::SetRecognitionMode(PayCardsRecognizerMode flag)
{
    _mode = flag;

    if (_mode&PayCardsRecognizerModeNumber) {
        _numberRecognizer = _serviceContainerPtr->resolve<INumberRecognizer>();
        if(auto numberRecognizer = _numberRecognizer.lock()) {
            numberRecognizer->SetRecognitionMode(flag);
        }
    }
    if (_mode&PayCardsRecognizerModeDate) {
        _dateRecognizer = _serviceContainerPtr->resolve<IDateRecognizer>();
        if(auto dateRecognizer = _dateRecognizer.lock()) {
            dateRecognizer->SetRecognitionMode(flag);
        }
    }
    if (_mode&PayCardsRecognizerModeName) {
        _nameRecognizer = _serviceContainerPtr->resolve<INameRecognizer>();
        if(auto nameRecognizer = _nameRecognizer.lock()) {
            nameRecognizer->SetRecognitionMode(flag);
        }
    }
}

void CRecognitionCore::Deploy()
{
    if (_mode == PayCardsRecognizerModeNone) return;
    
    if (auto numberRecognizer = _numberRecognizer.lock()) {
        numberRecognizer->SetDelegate(_delegate);
        if(!numberRecognizer->Deploy()) return;
    }
    
    if (auto dateRecognizer = _dateRecognizer.lock()) {
        dateRecognizer->SetDelegate(_delegate);
        if(!dateRecognizer->Deploy()) return;
    }
    
    if (auto nameRecognizer = _nameRecognizer.lock()) {
        nameRecognizer->SetDelegate(_delegate);
        if(!nameRecognizer->Deploy()) return;
    }
    
    _deployed = true;
}

void CRecognitionCore::SetOrientation(PayCardsRecognizerOrientation orientation)
{
    _orientation = orientation;
}

cv::Rect CRecognitionCore::CalcWorkingArea(cv::Size frameSize, int captureAreaWidth)
{
    if(auto edgesDetector = _edgesDetector.lock()) {
        return edgesDetector->CalcWorkingArea(frameSize, captureAreaWidth, _orientation);
    }
    
    return Rect(0,0,0,0);
}

void CRecognitionCore::SetIdle(bool isIdle)
{
    if (!isIdle) {
        if(auto recognitionResult = _recognitionResult.lock()) {
            std::unique_lock<std::mutex> lock(_isBusyMutex);
            while (_isBusy)
                _isBusyVar.wait(lock);
            recognitionResult->Reset();
        }
    }
    
    _isIdle.store(isIdle);
}

bool CRecognitionCore::IsIdle() const
{
    return _isIdle.load();
}

void CRecognitionCore::ResetResult()
{
    if(auto recognitionResult = _recognitionResult.lock()) {
        std::unique_lock<std::mutex> lock(_isBusyMutex);
        while (_isBusy)
            _isBusyVar.wait(lock);
        recognitionResult->Reset();
    }
}

void CRecognitionCore::SetPathNumberRecognitionModel(const string& path)
{
    if (auto numberRecognizer = _numberRecognizer.lock()) {
        numberRecognizer->SetPathNumberRecognitionModel(path);
    }
}

void CRecognitionCore::SetPathNumberRecognitionStruct(const string& path)
{
    if (auto numberRecognizer = _numberRecognizer.lock()) {
        numberRecognizer->SetPathNumberRecognitionStruct(path);
    }
}

void CRecognitionCore::SetPathNumberLocalizationXModel(const string& path)
{
    if (auto numberRecognizer = _numberRecognizer.lock()) {
        numberRecognizer->SetPathNumberLocalizationXModel(path);
    }
}

void CRecognitionCore::SetPathNumberLocalizationXStruct(const string& path)
{
    if (auto numberRecognizer = _numberRecognizer.lock()) {
        numberRecognizer->SetPathNumberLocalizationXStruct(path);
    }
}

void CRecognitionCore::SetPathNumberLocalizationYModel(const string& path)
{
    if (auto numberRecognizer = _numberRecognizer.lock()) {
        numberRecognizer->SetPathNumberLocalizationYModel(path);
    }
}

void CRecognitionCore::SetPathNumberLocalizationYStruct(const string& path)
{
    if (auto numberRecognizer = _numberRecognizer.lock()) {
        numberRecognizer->SetPathNumberLocalizationYStruct(path);
    }
}

void CRecognitionCore::SetPathDateRecognitionModel(const string& path)
{
    if (auto dateRecognizer = _dateRecognizer.lock()) {
        dateRecognizer->SetPathDateRecognitionModel(path);
    }
}

void CRecognitionCore::SetPathDateRecognitionStruct(const string& path)
{
    if (auto dateRecognizer = _dateRecognizer.lock()) {
        dateRecognizer->SetPathDateRecognitionStruct(path);
    }
}

void CRecognitionCore::SetPathDateLocalization0Model(const string& path)
{
    if (auto dateRecognizer = _dateRecognizer.lock()) {
        dateRecognizer->SetPathDateLocalization0Model(path);
    }
}

void CRecognitionCore::SetPathDateLocalization0Struct(const string& path)
{
    if (auto dateRecognizer = _dateRecognizer.lock()) {
        dateRecognizer->SetPathDateLocalization0Struct(path);
    }
}

void CRecognitionCore::SetPathDateLocalization1Model(const string& path)
{
    if (auto dateRecognizer = _dateRecognizer.lock()) {
        dateRecognizer->SetPathDateLocalization1Model(path);
    }
}

void CRecognitionCore::SetPathDateLocalization1Struct(const string& path)
{
    if (auto dateRecognizer = _dateRecognizer.lock()) {
        dateRecognizer->SetPathDateLocalization1Struct(path);
    }
}

void CRecognitionCore::SetPathDateLocalizationViola(const string& path)
{
    if (auto dateRecognizer = _dateRecognizer.lock()) {
        dateRecognizer->SetPathDateLocalizationViola(path);
    }
}

void CRecognitionCore::SetPathNameYLocalizationViola(const string& path)
{
    if (auto nameRecognizer = _nameRecognizer.lock()) {
        nameRecognizer->SetPathNameYLocalizationViola(path);
    }
}

void CRecognitionCore::SetPathNameLocalizationXModel(const string& path)
{
    if (auto nameRecognizer = _nameRecognizer.lock()) {
        nameRecognizer->SetPathNameLocalizationXModel(path);
    }
}

void CRecognitionCore::SetPathNameLocalizationXStruct(const string& path)
{
    if (auto nameRecognizer = _nameRecognizer.lock()) {
        nameRecognizer->SetPathNameLocalizationXStruct(path);
    }
}

void CRecognitionCore::SetPathNameSpaceCharModel(const string& path)
{
    if (auto nameRecognizer = _nameRecognizer.lock()) {
        nameRecognizer->SetPathNameSpaceCharModel(path);
    }
}

void CRecognitionCore::SetPathNameSpaceCharStruct(const string& path)
{
    if (auto nameRecognizer = _nameRecognizer.lock()) {
        nameRecognizer->SetPathNameSpaceCharStruct(path);
    }
}

void CRecognitionCore::SetPathNameListTxt(const string& path)
{
    if (auto nameRecognizer = _nameRecognizer.lock()) {
        nameRecognizer->SetPathNameListTxt(path);
    }
}

void CRecognitionCore::ProcessFrame(DetectedLineFlags& edgeFlags, void* bufferY,
                                    void* bufferUV, size_t bufferSizeY, size_t bufferSizeUV)
{
    if (!IsIdle() && _deployed) {

        Mat frameMatY = cv::Mat(1280, 720, CV_8UC1, bufferY); //put buffer in open cv, no memory copied
        
        if(auto edgesDetector = _edgesDetector.lock()) {
            if(auto recognitionResult = _recognitionResult.lock()) {
                if(auto frameStorage = _frameStorage.lock()) {
                    Mat bordersFrame;
                    edgeFlags = edgesDetector->DetectEdges(frameMatY, _edges, bordersFrame);
                    if (edgeFlags&DetectedLineTopFlag &&  edgeFlags&DetectedLineBottomFlag
                        && edgeFlags&DetectedLineLeftFlag && edgeFlags&DetectedLineRightFlag) {
                        std::unique_lock<std::mutex> lock(_isBusyMutex);
                        if (!_isBusy) {
                            _isBusy = true;
                            _bufferSizeY = bufferSizeY;
                            if (!(recognitionResult->GetRecognitionStatus() & RecognitionStatusNumber)) {
                                frameStorage->SetRawY(frameMatY.data, bufferUV, _edges, _orientation);
                            }
                            bordersFrame.copyTo(_currentFrame);
                            
                            std::thread thread;
                            thread = std::thread( [this] { this->ProcessFrameThreaded(); } );
                            
                            sched_param sch;
                            int policy;
                            pthread_getschedparam(thread.native_handle(), &policy, &sch);
                            sch.sched_priority = 99;
                            pthread_setschedparam(thread.native_handle(), SCHED_FIFO, &sch);
                            
                            thread.detach();
                        }
                    }
                }
            }
        }
    }
}

void CRecognitionCore::FinishRecognition()
{
    if(auto frameStorage = _frameStorage.lock()) {
        frameStorage->PopFrame();
        std::unique_lock<std::mutex> lock(_isBusyMutex);
        _isBusy = false;
        _isBusyVar.notify_one();
    }
}

void CRecognitionCore::ProcessFrameThreaded()
{
    if(auto frameStorage = _frameStorage.lock()) {
        if(frameStorage->SetRawFrame(_currentFrame, _edges, _orientation)) {
            Recognize();
        }
        else {
            std::unique_lock<std::mutex> lock(_isBusyMutex);
            _isBusy = false;
            _isBusyVar.notify_one();
        }
    }
}

void CRecognitionCore::Recognize()
{
    if (auto recognitionResult = _recognitionResult.lock()) {
        
        // number
        if (_mode&PayCardsRecognizerModeNumber &&
            !(recognitionResult->GetRecognitionStatus()&RecognitionStatusNumber)) {
            if (!RecognizeNumber()) {
                FinishRecognition();
                return;
            }
        }
        
        // date
        if (_mode&PayCardsRecognizerModeDate &&
            !(recognitionResult->GetRecognitionStatus() & RecognitionStatusDate) &&
            dateRecognitionAttemptsCount < kDateRecognitionAttempts) {
            if (!RecognizeDate()) {
                FinishRecognition();
                return;
            }
        }
        
        if (_mode&PayCardsRecognizerModeDate || _mode&PayCardsRecognizerModeNumber) {
            _delegate->RecognitionDidFinish(recognitionResult, (PayCardsRecognizerMode)(PayCardsRecognizerModeNumber|PayCardsRecognizerModeDate));
            
            if(_mode&PayCardsRecognizerModeGrabCardImage) {
                auto cardMat = CaptureView();
                if(!cardMat.empty()) {
                    _delegate->CardImageDidExtract(cardMat);
                }
            }
        }
        
        // name
        if (_mode&PayCardsRecognizerModeName &&
            !(recognitionResult->GetRecognitionStatus() & RecognitionStatusName)) {
            RecognizeName();
        }
        
        _delegate->RecognitionDidFinish(recognitionResult, PayCardsRecognizerModeName);
        
        _isIdle.store(true);
        FinishRecognition();
        dateRecognitionAttemptsCount = 0;
        SetTorchStatus(false);
    }
}

bool CRecognitionCore::RecognizeName()
{
    shared_ptr<INeuralNetworkResultList> result;
    
    if(auto frameStorage = _frameStorage.lock()) {
        if(auto nameRecognizer = _nameRecognizer.lock()) {
            if(auto recognitionResult = _recognitionResult.lock()) {
                Mat frame;
                frameStorage->GetCurrentFrame(frame);
                
                vector<cv::Mat> samples;
                std::string postprocessedName = "";
                result = nameRecognizer->Process(frame, samples, postprocessedName);
                
                if (result) {
                    recognitionResult->SetNameResult(result);
                    recognitionResult->SetPostprocessedName(postprocessedName);
                }
            }
        }
    }
    
    return result != nullptr;
}

bool CRecognitionCore::RecognizeNumber()
{
    cv::Rect boundingRect;
    shared_ptr<INeuralNetworkResultList> result;
    
    if(auto frameStorage = _frameStorage.lock()) {
        if(auto numberRecognizer = _numberRecognizer.lock()) {
            if(auto recognitionResult = _recognitionResult.lock()) {
                Mat frame;
                frameStorage->GetCurrentFrame(frame);
                result = numberRecognizer->Process(frame, boundingRect);
                if(result) {
                    recognitionResult->SetNumberResult(result);
                    recognitionResult->SetNumberRect(boundingRect);
                    recognitionResult->SetCardImage(frame.clone());
                }
                if(auto torchManager = _torchManager.lock()) {
                    torchManager->IncrementCounter();
                }
            }
        }
    }

    return result != nullptr;
}

bool CRecognitionCore::RecognizeDate()
{
    cv::Rect boundingRect;
    shared_ptr<INeuralNetworkResultList> result;
    if(auto frameStorage = _frameStorage.lock()) {
        if(auto dateRecognizer = _dateRecognizer.lock()) {
            if(auto recognitionResult = _recognitionResult.lock()) {
                Mat frame;
                frameStorage->GetCurrentFrame(frame);
                
                vector<cv::Mat> samples;
                result = dateRecognizer->Process(frame, samples, boundingRect);
                
                dateRecognitionAttemptsCount++;
                if (result) {
                    _isIdle.store(true);
                    recognitionResult->SetDateResult(result);
                    recognitionResult->SetDateRect(boundingRect);
                }
            }
        }
    }
    
    return result != nullptr;
}

void CRecognitionCore::SetTorchStatus(bool status)
{
    if(auto torchManager = _torchManager.lock()) {
        torchManager->SetStatus(status);
    }
}


cv::Mat CRecognitionCore::CaptureView()
{
    Mat normalizedMat;
    
    if(auto frameStorage = _frameStorage.lock()) {
        if(auto edgesDetector = _edgesDetector.lock()) {
            size_t bufferLen = 720 * 1280 * 3 / 2 ;
            uint8_t *uPlane, *vPlane;
            void* imgBuffer = malloc(bufferLen);
            
            memcpy(imgBuffer, frameStorage->GetYMat(), _bufferSizeY);
            
            size_t planeWidth = 360;
            size_t planeHeight = 640;
            uint8_t *planeBaseAddress = (uint8_t *)frameStorage->GetUVMat();
            size_t planeSize = planeWidth * planeHeight;
#ifdef __APPLE__
            // Convert Y'UV420sp to Y'UV420p
            uPlane = (uint8_t *)malloc(planeSize);
            vPlane = (uint8_t *)malloc(planeSize);
            
            for (uint32_t i = 0; i < (planeWidth * planeHeight / 4); i++) {
                uint8_t *uvSrc = &planeBaseAddress[i * 8];
                uint8_t *uDest = &uPlane[i * 4];
                uint8_t *vDest = &vPlane[i * 4];
                uint8x8x2_t loaded = vld2_u8(uvSrc);
                vst1_u8(uDest, loaded.val[0]);
                vst1_u8(vDest, loaded.val[1]);
            }
#else
            // Convert YV12 to Y'UV420p
            uPlane = planeBaseAddress;
            vPlane = &planeBaseAddress[planeSize];
#endif
            
            memcpy(((unsigned char *)imgBuffer) + _bufferSizeY, uPlane, planeSize);
            memcpy(((unsigned char *)imgBuffer) + _bufferSizeY + planeSize, vPlane, planeSize);
            
            Mat yuv = Mat(1280 + 1280/2, 720, CV_8UC1, imgBuffer);
            
            Mat rgb;
            cvtColor(yuv, rgb, CV_YUV2RGB_I420);
            
            
            Mat refinedMat = rgb(edgesDetector->GetInternalWindowRect());
            
            vector<ParametricLine> lines = frameStorage->GetEdges();
            frameStorage->NormalizeMatrix(refinedMat, lines, normalizedMat);
            
#ifdef __APPLE__
            free(uPlane);
            free(vPlane);
#endif
            free(imgBuffer);
            
            PayCardsRecognizerOrientation orientation = frameStorage->GetYUVOrientation();
            
            if (orientation != PayCardsRecognizerOrientationPortraitUpsideDown &&
                orientation != PayCardsRecognizerOrientationPortrait) {
                
                CUtils::RotateMatrix90n(normalizedMat, normalizedMat, 90);
            }
            
            cv::resize(normalizedMat, normalizedMat, cv::Size(660,416));
        }
    }
    
    return normalizedMat;
}
