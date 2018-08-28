//
//  DateRecognizer.cpp
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 12/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#include "DateRecognizer.h"
#include "INeuralNetworkObjectFactory.h"
#include "IServiceContainer.h"
#include "IRecognitionCoreDelegate.h"
#include "Utils.h"

static const cv::Rect dateWindowRect(257,282,210,65);

CDateRecognizer::CDateRecognizer(shared_ptr<IServiceContainer> container) : _container(container)
{
    if(auto container = _container.lock()) {
        _factory = container->resolve<INeuralNetworkObjectFactory>();
    }
}

CDateRecognizer::~CDateRecognizer()
{
}

void CDateRecognizer::SetRecognitionMode(PayCardsRecognizerMode flag)
{
    _mode = flag;
}

void CDateRecognizer::SetDelegate(const shared_ptr<IRecognitionCoreDelegate>& delegate)
{
    _delegate = delegate;
}

void CDateRecognizer::SetPathDateRecognitionModel(const string& path)
{
    _pathDateRecognitionModel = path;
}

void CDateRecognizer::SetPathDateRecognitionStruct(const string& path)
{
    _pathDateRecognitionStruct = path;
}

void CDateRecognizer::SetPathDateLocalization0Model(const string& path)
{
    _pathDateLocalization0Model = path;
}

void CDateRecognizer::SetPathDateLocalization0Struct(const string& path)
{
    _pathDateLocalization0Struct = path;
}

void CDateRecognizer::SetPathDateLocalization1Model(const string& path)
{
    _pathDateLocalization1Model = path;
}

void CDateRecognizer::SetPathDateLocalization1Struct(const string& path)
{
    _pathDateLocalization1Struct = path;
}

void CDateRecognizer::SetPathDateLocalizationViola(const string& path)
{
    _pathDateLocalizationViola = path;
}

bool CDateRecognizer::Deploy()
{
    if(auto factory = _factory.lock()) {
        bool cascadeFlag = false;
        
        if (_pathDateLocalizationViola.length() > 0) {
            cascadeFlag =  _dateCascade.load(_pathDateLocalizationViola);
        }
        
        _dateRecognitionNeuralNetwork = factory->CreateNeuralNetwork("", _pathDateRecognitionStruct, _pathDateRecognitionModel);
        _dateLocalizationNeuralNetworkL1 = factory->CreateNeuralNetwork("", _pathDateLocalization1Struct, _pathDateLocalization1Model);
        _dateLocalizationNeuralNetworkL0 = factory->CreateNeuralNetwork("", _pathDateLocalization0Struct, _pathDateLocalization0Model);
        
        return _dateRecognitionNeuralNetwork->IsDeployed() && _dateLocalizationNeuralNetworkL1->IsDeployed() && _dateLocalizationNeuralNetworkL0->IsDeployed() && cascadeFlag && _pathDateLocalizationViola.length() > 0;
    }
    
    return false;
}

void CDateRecognizer::Predict(const vector<Mat>& matrixes,
                              shared_ptr<INeuralNetworkResultList>& neuralNetworkResultList, const shared_ptr<INeuralNetwork>& neuralNetwork)
{
    if(auto factory = _factory.lock()) {
        shared_ptr<INeuralNetworkDatumList> neuralNetworkDatumList = factory->CreateNeuralNetworkDatumList();
        
        for(Mat matrix : matrixes) {
            shared_ptr<INeuralNetworkDatum> neuralNetworkDatum = factory->CreateNeuralNetworkDatum(matrix);
            neuralNetworkDatumList->PushBack(neuralNetworkDatum);
        }
        
        neuralNetwork->Predict(neuralNetworkDatumList, neuralNetworkResultList);
    }
}


void CDateRecognizer::PredictDate(const vector<Mat>& digits, shared_ptr<INeuralNetworkResultList>& neuralNetworkResultList, float& meanConfidence, const shared_ptr<INeuralNetwork>& neuralNetwork)
{
    if(auto factory = _factory.lock()) {
        shared_ptr<INeuralNetworkDatumList> neuralNetworkDatumList = factory->CreateNeuralNetworkDatumList();
        int count = 0;
        for(auto it = begin(digits); it < end(digits); ++it) {
            
            shared_ptr<INeuralNetworkDatum> neuralNetworkDatum = factory->CreateNeuralNetworkDatum(*it);
            neuralNetworkDatumList->PushBack(neuralNetworkDatum);
            count++;
        }
        
        neuralNetwork->Predict(neuralNetworkDatumList, neuralNetworkResultList);
        
        meanConfidence = neuralNetworkResultList->GetMeanConfidence();
    }
}

bool CDateRecognizer::ValidateDate(const shared_ptr<INeuralNetworkResultList>& dateResult, bool& stop)
{
    float minConfidence = 1.0;
    for(auto it=dateResult->Begin(); it != dateResult->End(); ++it)
    {
        shared_ptr<INeuralNetworkResult> result = *it;
        minConfidence = MIN(minConfidence, result->GetMaxProbability());
    }
    
    if (minConfidence < 0.95) {
        return false;
    }
    
    shared_ptr<INeuralNetworkResult> firstResult = dateResult->GetAtIndex(0);
    
    
    if (firstResult->GetMaxIndex() > 1) {
        return false;
    }
    
    shared_ptr<INeuralNetworkResult> secondResult = dateResult->GetAtIndex(1);
    if (firstResult->GetMaxIndex() == 1 && secondResult->GetMaxIndex() > 2) {
        return false;
    }
    
    if (firstResult->GetMaxIndex() == 0 && secondResult->GetMaxIndex() == 0) {
        return false;
    }
    
    shared_ptr<INeuralNetworkResult> thirdResult = dateResult->GetAtIndex(2);
    if (thirdResult->GetMaxIndex() == 0 || thirdResult->GetMaxIndex() > 2) {
        return false;
    }
    
    if (firstResult->GetMaxIndex() == 1 && thirdResult->GetMaxIndex() == 1
            && secondResult->GetMaxIndex() == 1 && dateResult->GetAtIndex(3)->GetMaxIndex() == 1) {
        return false;
    }

    return true;
}

bool CDateRecognizer::RefineDateLocationL0(Mat& dateMat, const vector<cv::Point> dateCenters, vector<cv::Rect>& refinedDateRects)
{
    // size of the date rects we got while rough localization
    // extended by padding, the size is used in regression NN
    const cv::Size rectSize = cv::Size(242,60);
    
    // prepare date matrixes for refinment
    for (cv::Point center : dateCenters) {
        cv::Rect extendedRect = cv::Rect(center.x-rectSize.width/2, center.y-rectSize.height/2, rectSize.width,rectSize.height);

        if (!CUtils::ValidateROI(dateMat, extendedRect)) return false;
        
        Mat roiMat = dateMat(extendedRect);
        Mat roiMatLow;
        resize(roiMat, roiMatLow, cv::Size(rectSize.width/2, rectSize.height/2));
        
        if(auto factory = _factory.lock()) {
        
            shared_ptr<INeuralNetworkResultList> neuralNetworkResultList = factory->CreateNeuralNetworkResultList();
            Predict({roiMatLow}, neuralNetworkResultList, _dateLocalizationNeuralNetworkL0);
            
            shared_ptr<INeuralNetworkResult> result = neuralNetworkResultList->GetAtIndex(0);
            vector<pair<int, float>> data = result->GetRawResult();
            
            // apply refinment to original rects
            cv::Point shift = cv::Point(cvRound(data.at(0).second*72.0), cvRound(data.at(1).second*16.0));
            cv::Rect refinedRect = cv::Rect(shift.x*2.0 + extendedRect.x, shift.y*2.0 + extendedRect.y, 98, 28);
            
            refinedDateRects.push_back(refinedRect);
        }
    }
    
    return true;
}

bool CDateRecognizer::RefineDateLocationL1(Mat& dateMat, const vector<cv::Point> dateCenters, vector<cv::Rect>& refinedDateRects)
{
    // size of the date rects we got after rough localization
    // extended by padding, the size is used in regression NN
    const cv::Size rectSize = cv::Size(110,36);
    
    if(auto factory = _factory.lock()) {
        // prepare date matrixes for refinment
        for (cv::Point center : dateCenters) {
            
            cv::Rect extendedRect = cv::Rect(center.x-rectSize.width/2, center.y-rectSize.height/2, rectSize.width,rectSize.height);
            
            shared_ptr<INeuralNetworkResultList> neuralNetworkResultList = factory->CreateNeuralNetworkResultList();
            
            if (!CUtils::ValidateROI(dateMat, extendedRect)) return false;
            
            Predict({dateMat(extendedRect)}, neuralNetworkResultList, _dateLocalizationNeuralNetworkL1);
            
            shared_ptr<INeuralNetworkResult> result = neuralNetworkResultList->GetAtIndex(0);
            vector<pair<int, float>> data = result->GetRawResult();
            
            // apply refinment to original rects
            cv::Point shift = cv::Point(cvRound(data.at(0).second*12.0), cvRound(data.at(1).second*8.0));
            cv::Rect refinedRect = cv::Rect(extendedRect.x + shift.x, extendedRect.y + shift.y, 98, 28);
            
            refinedDateRects.push_back(refinedRect);
        }
        
        return true;
    }
    
    return false;
}

shared_ptr<INeuralNetworkResultList> CDateRecognizer::Process(cv::Mat& frame, vector<cv::Mat>& samples, cv::Rect& boundingRect)
{
    shared_ptr<INeuralNetworkResultList> finalResult = nullptr;
    
    Mat dateMat = frame(dateWindowRect);

    std::vector<cv::Rect> dateRects;
    std::vector<int> rejectLevels;
    std::vector<double> levelWeights;
    _dateCascade.detectMultiScale(dateMat, dateRects, rejectLevels, levelWeights, 1.01, 1,
            0|CV_HAAR_SCALE_IMAGE, cv::Size(96, 26), cv::Size(100, 30), true);

    // find rough location of the rects using Viola-Jones cascade
    if (dateRects.size() == 0) return nullptr;
    
    // we are going to process only first rectsLimit rect
    const int rectsLimit = 1;

    if (dateRects.size() > rectsLimit) {
        std::vector<cv::Rect> newDateRects(rectsLimit);
        partial_sort_copy(dateRects.begin(), dateRects.end(), newDateRects.begin(), newDateRects.end(),
                [&levelWeights,&dateRects](const cv::Rect &r1, const cv::Rect &r2) {
            auto pos1 = find(dateRects.begin(), dateRects.end(), r1) - dateRects.begin();
            auto pos2 = find(dateRects.begin(), dateRects.end(), r2) - dateRects.begin();
            if (levelWeights[pos1] != levelWeights[pos2]) return levelWeights[pos1] > levelWeights[pos2];
            if (r1.x != r2.x) return r1.x < r2.x;
            if (r1.y != r2.y) return r1.y < r2.y;
            if (r1.width != r2.width) return r1.width < r2.width;
            return r1.height < r2.height;
        });
        dateRects = std::move(newDateRects);
    }
    
    // create vector of rects centers, we use it to refine location by regression NN
    vector<cv::Point> rectCentersL0;
    for (cv::Rect& rect : dateRects) {
        cv::Point center = cv::Point(rect.x + rect.width/2 + dateWindowRect.x, rect.y + rect.height/2 + dateWindowRect.y);
        rectCentersL0.push_back(center);
    }
    
    vector<cv::Rect> refinedRectsL0;
    
    if(!RefineDateLocationL0(frame, rectCentersL0, refinedRectsL0)) return nullptr;
    
    // extend the initial date matrix, because we are going to refine the location of the rects
    // so the correct location can be outside of the initial matrix
    const int padding = 10;
    cv::Rect extendedRect = cv::Rect(dateWindowRect.x - padding, dateWindowRect.y - padding,
                                     dateWindowRect.width + padding*2, dateWindowRect.height + padding*2);
    
    // create vector of rects centers, we use it to refine location by regression NN
    vector<cv::Point> rectCentersL1;
    for (cv::Rect& rect : refinedRectsL0) {
        cv::Point center = cv::Point(rect.x + rect.width/2, rect.y + rect.height/2);
        rectCentersL1.push_back(center);
    }
    
    if (!CUtils::ValidateROI(frame, extendedRect)) return nullptr;
    Mat refinmentMat = frame(extendedRect);
    
    vector<cv::Rect> refinedRects;
    if(!RefineDateLocationL1(frame, rectCentersL1, refinedRects)) return nullptr;
    
    // date recognition
    float maxConfidence = 0.0;
    cv::Rect rect0, rect1, rect2, rect3;
    
    if(auto factory = _factory.lock()) {
        for(cv::Rect rect : refinedRects) {
            cv::Point center = cv::Point(rect.x + rect.width/2, rect.y + rect.height/2 + 2); // magic + 2
            rect0 = cv::Rect(center.x - 49, center.y - 15, 20, 29);
            rect1 = cv::Rect(center.x - 29, center.y - 15, 20, 29);
            rect2 = cv::Rect(center.x + 11, center.y - 15, 20, 29);
            rect3 = cv::Rect(center.x + 31, center.y - 15, 20, 29);
            
            if (!CUtils::ValidateROI(frame, rect0) || !CUtils::ValidateROI(frame, rect1) ||
                !CUtils::ValidateROI(frame, rect2) || !CUtils::ValidateROI(frame, rect3)) {
                return nullptr;
            }
            
            vector<Mat> digits {frame(rect0), frame(rect1), frame(rect2), frame(rect3)};
            
            shared_ptr<INeuralNetworkResultList> neuralNetworkResultDigits;
            neuralNetworkResultDigits = factory->CreateNeuralNetworkResultList();
            
            float meanConfidence = 0.0;
            
            PredictDate(digits, neuralNetworkResultDigits, meanConfidence, _dateRecognitionNeuralNetwork);
            
            bool stop = false;
            if (ValidateDate(neuralNetworkResultDigits,stop) && meanConfidence > maxConfidence) {
                if (stop) return nullptr;
                
                maxConfidence = meanConfidence;
                finalResult = neuralNetworkResultDigits;
                boundingRect = rect;
            }
        }
    }
    
    return finalResult;
}


