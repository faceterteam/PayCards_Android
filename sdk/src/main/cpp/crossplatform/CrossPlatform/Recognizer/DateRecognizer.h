//
//  DateRecognizer.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 12/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef DateRecognizer_h
#define DateRecognizer_h

#include "IDateRecognizer.h"

class IServiceContainer;
class INeuralNetworkObjectFactory;
class INeuralNetwork;

class CDateRecognizer : public IDateRecognizer
{
    
public:
    CDateRecognizer(shared_ptr<IServiceContainer> container);
    
    virtual ~CDateRecognizer();
    
public:

    virtual shared_ptr<INeuralNetworkResultList> Process(cv::Mat& frame, vector<cv::Mat>& samples, cv::Rect& boundingRect);
    virtual bool Deploy();

    virtual void SetRecognitionMode(PayCardsRecognizerMode flag);
    
    virtual void SetPathDateRecognitionModel(const string& path);
    virtual void SetPathDateRecognitionStruct(const string& path);
    
    virtual void SetPathDateLocalization0Model(const string& path);
    virtual void SetPathDateLocalization0Struct(const string& path);
    
    virtual void SetPathDateLocalization1Model(const string& path);
    virtual void SetPathDateLocalization1Struct(const string& path);
    
    virtual void SetPathDateLocalizationViola(const string& path);
    
    virtual void SetDelegate(const shared_ptr<IRecognitionCoreDelegate>& delegate);

private:
    
    void Predict(const vector<cv::Mat>& matrixes, shared_ptr<INeuralNetworkResultList>& neuralNetworkResultList,
                 const shared_ptr<INeuralNetwork>& neuralNetwork);

    void PredictDate(const vector<cv::Mat>& digits, shared_ptr<INeuralNetworkResultList>& neuralNetworkResultList, float& meanConfidence, const shared_ptr<INeuralNetwork>& neuralNetwork);
    
    bool ValidateDate(const shared_ptr<INeuralNetworkResultList>& dateResult, bool& stop);

    bool RefineDateLocationL0(cv::Mat& dateMat, const vector<cv::Point> dateCenters, vector<cv::Rect>& refinedDateRects);
    bool RefineDateLocationL1(cv::Mat& dateMat, const vector<cv::Point> dateCenters, vector<cv::Rect>& refinedDateRects);
    


private:
    
    weak_ptr<IServiceContainer> _container;
    weak_ptr<IRecognitionCoreDelegate> _delegate;
    weak_ptr<INeuralNetworkObjectFactory> _factory;
    
    shared_ptr<INeuralNetwork> _dateRecognitionNeuralNetwork;
    shared_ptr<INeuralNetwork> _dateLocalizationNeuralNetworkL0;
    shared_ptr<INeuralNetwork> _dateLocalizationNeuralNetworkL1;
    
    cv::CascadeClassifier _dateCascade;
    
    string _pathDateRecognitionModel;
    string _pathDateRecognitionStruct;
    string _pathDateLocalization0Model;
    string _pathDateLocalization0Struct;
    string _pathDateLocalization1Model;
    string _pathDateLocalization1Struct;
    string _pathDateLocalizationViola;
    
    PayCardsRecognizerMode _mode;
};


#endif /* DateRecognizer_h */
