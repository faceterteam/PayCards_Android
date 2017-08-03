//
//  IDateRecognizer.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 11/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef IDateRecognizer_h
#define IDateRecognizer_h

#include "IBaseObj.h"
#include "Enums.h"

class INeuralNetworkResultList;
class IRecognitionCoreDelegate;

using namespace std;

class IDateRecognizer : public IBaseObj
{
public:
    
    virtual ~IDateRecognizer() {}
    
public:
    
    virtual shared_ptr<INeuralNetworkResultList> Process(cv::Mat& frame, vector<cv::Mat>& samples, cv::Rect& boundingRect) = 0;
    virtual bool Deploy() = 0;
    
    virtual void SetRecognitionMode(PayCardsRecognizerMode flag) = 0;
    
    virtual void SetPathDateRecognitionModel(const string& path) = 0;
    virtual void SetPathDateRecognitionStruct(const string& path) = 0;
    
    virtual void SetPathDateLocalization0Model(const string& path) = 0;
    virtual void SetPathDateLocalization0Struct(const string& path) = 0;
    
    virtual void SetPathDateLocalization1Model(const string& path) = 0;
    virtual void SetPathDateLocalization1Struct(const string& path) = 0;
    
    virtual void SetPathDateLocalizationViola(const string& path) = 0;

    virtual void SetDelegate(const shared_ptr<IRecognitionCoreDelegate>& delegate) = 0;
    
};


#endif /* IDateRecognizer_h */
