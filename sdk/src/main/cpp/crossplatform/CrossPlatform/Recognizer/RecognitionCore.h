//
//  RecognitionCore.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 12/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef RecognitionCore_h
#define RecognitionCore_h


#include <atomic>
#include <condition_variable>
#include <mutex>
#include "IRecognitionCore.h"
#include "IFrameStorage.h"
#include "IRecognitionCoreDelegate.h"

class IEdgesDetector;
class IRecognitionResult;
class IServiceContainer;
class INumberRecognizer;
class IDateRecognizer;
class ITorchManager;
class ITorchDelegate;
class INameRecognizer;


class CRecognitionCore : public IRecognitionCore
{
    
public:
    CRecognitionCore(const shared_ptr<IRecognitionCoreDelegate>& delegate, const shared_ptr<ITorchDelegate>& torchDelegate);
    
    virtual ~CRecognitionCore();
    
public:
    
    virtual void SetRecognitionMode(PayCardsRecognizerMode flag);
    
    virtual void Deploy();
    
    virtual void SetPathNumberRecognitionModel(const string& path);
    virtual void SetPathNumberRecognitionStruct(const string& path);
    
    virtual void SetPathNumberLocalizationXModel(const string& path);
    virtual void SetPathNumberLocalizationXStruct(const string& path);
    
    virtual void SetPathNumberLocalizationYModel(const string& path);
    virtual void SetPathNumberLocalizationYStruct(const string& path);
    
    virtual void SetPathDateRecognitionModel(const string& path);
    virtual void SetPathDateRecognitionStruct(const string& path);
    
    virtual void SetPathDateLocalization0Model(const string& path);
    virtual void SetPathDateLocalization0Struct(const string& path);
    
    virtual void SetPathDateLocalization1Model(const string& path);
    virtual void SetPathDateLocalization1Struct(const string& path);
    
    virtual void SetPathDateLocalizationViola(const string& path);
    
    virtual void SetPathNameYLocalizationViola(const string& path);
    
    virtual void SetPathNameLocalizationXModel(const string& path);
    virtual void SetPathNameLocalizationXStruct(const string& path);

    virtual void SetPathNameSpaceCharModel(const string& path);
    virtual void SetPathNameSpaceCharStruct(const string& path);
    
    virtual void SetPathNameListTxt(const string& path);
    
    virtual void SetOrientation(PayCardsRecognizerOrientation orientation);
    
    virtual bool IsIdle() const;
    virtual void SetIdle(bool isIdle);
    virtual void ResetResult();
    
    virtual void SetTorchStatus(bool status);
    
    virtual cv::Rect CalcWorkingArea(cv::Size frameSize, int captureAreaWidth);
    
    virtual void ProcessFrame(DetectedLineFlags& edgeFlags, void* bufferY, void* bufferUV, size_t bufferSizeY, size_t bufferSizeUV);

private:
    cv::Mat CaptureView();
    void ProcessFrameThreaded();
    void Recognize();
    bool RecognizeNumber();
    bool RecognizeDate();
    bool RecognizeName();
    void FinishRecognition();
    
private:
    
    shared_ptr<IRecognitionCoreDelegate> _delegate;
    shared_ptr<IServiceContainer> _serviceContainerPtr;
    
    weak_ptr<IFrameStorage> _frameStorage;
    weak_ptr<IEdgesDetector> _edgesDetector;
    weak_ptr<IRecognitionResult> _recognitionResult;
    
    weak_ptr<IDateRecognizer> _dateRecognizer;
    weak_ptr<INumberRecognizer> _numberRecognizer;
    weak_ptr<INameRecognizer> _nameRecognizer;
    weak_ptr<ITorchManager> _torchManager;
    
    atomic<bool> _isIdle;

    std::mutex _isBusyMutex;
    std::condition_variable _isBusyVar;
    bool _isBusy;

    cv::Mat _currentFrame;
    vector<ParametricLine> _edges;
    size_t _bufferSizeY;
    
    PayCardsRecognizerOrientation _orientation;
    
    PayCardsRecognizerMode _mode;
    
    bool _deployed;
};

#endif /* RecognitionCore_h */
