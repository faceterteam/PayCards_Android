//
//  NumberRecognizer.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 12/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef NumberRecognizer_h
#define NumberRecognizer_h

#include "INumberRecognizer.h"

class INeuralNetworkResultList;
class IServiceContainer;
class INeuralNetwork;
class INeuralNetworkObjectFactory;

class CNumberRecognizer : public INumberRecognizer
{
    
public:
    CNumberRecognizer(const shared_ptr<IServiceContainer>& container);
    
    virtual ~CNumberRecognizer();
    
public:
    
    virtual shared_ptr<INeuralNetworkResultList> Process(cv::Mat& matrix, cv::Rect& boundingRect);
    
    virtual bool Deploy();

    virtual void SetRecognitionMode(PayCardsRecognizerMode flag);
    
    virtual void SetPathNumberRecognitionModel(const string& path);
    virtual void SetPathNumberRecognitionStruct(const string& path);
    
    virtual void SetPathNumberLocalizationXModel(const string& path);
    virtual void SetPathNumberLocalizationXStruct(const string& path);
    
    virtual void SetPathNumberLocalizationYModel(const string& path);
    virtual void SetPathNumberLocalizationYStruct(const string& path);
    
    virtual void SetDelegate(const shared_ptr<IRecognitionCoreDelegate>& delegate);

private:
    
    cv::Mat HistY(const cv::Mat& blockMat);
    void Predict(const vector<cv::Mat>& matrixes, shared_ptr<INeuralNetworkResultList>& neuralNetworkResultList, const shared_ptr<INeuralNetwork>& neuralNetwork);
    vector<cv::Mat> SplitBlock(const cv::Mat& mat, int xPos, int yPos, int offset,
                           int digitSpace, int xPadding, int yPadding, vector<cv::Rect>& digitsRects);
    bool PreLocalize(cv::Mat& numberWindow, cv::Mat& matrix, vector<cv::Point>& points);

    shared_ptr<INeuralNetworkResultList> ProcessMatrixFinal(cv::Mat& numberWindow,
                                                            const vector<cv::Point>& points,
                                                            const shared_ptr<INeuralNetwork>&
                                                            neuralNetwork,
                                                            cv::Point paddingPoint,
                                                            cv::Rect& boundingRect);
    
    bool ValidateNumber(const shared_ptr<INeuralNetworkResultList>& result);
    bool CheckSum(const shared_ptr<INeuralNetworkResultList>& result);
    
    void PredictThreaded(const vector<cv::Mat>& matrixes, shared_ptr<INeuralNetworkResultList>& neuralNetworkResultList,
                         const shared_ptr<INeuralNetwork>& neuralNetwork);
    
private:
    
    weak_ptr<IServiceContainer> _container;

    weak_ptr<IRecognitionCoreDelegate> _delegate;
    
    weak_ptr<INeuralNetworkObjectFactory> _factory;
    
    shared_ptr<INeuralNetwork> _localizationNeuralNetworkY;
    shared_ptr<INeuralNetwork> _localizationNeuralNetworkX;
    
    shared_ptr<INeuralNetwork> _recognitionNeuralNetwork;
    
    string _pathNumberRecognitionModel;
    string _pathNumberRecognitionStruct;
    string _pathNumberLocalizationXModel;
    string _pathNumberLocalizationXStruct;
    string _pathNumberLocalizationYModel;
    string _pathNumberLocalizationYStruct;
    
    PayCardsRecognizerMode _mode;
};

#endif /* NumberRecognizer_h */
