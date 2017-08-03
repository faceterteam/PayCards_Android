//
//  INumberRecognizer.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 11/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef INumberRecognizer_h
#define INumberRecognizer_h

#include "IBaseObj.h"
#include "Enums.h"

class INeuralNetworkResultList;
class IRecognitionCoreDelegate;

using namespace std;

class INumberRecognizer : public IBaseObj
{
public:
    
    virtual ~INumberRecognizer() {}
    
public:
  
    virtual shared_ptr<INeuralNetworkResultList> Process(cv::Mat& matrix, cv::Rect& boundingRect) = 0;
    virtual bool Deploy() = 0;
    
    virtual void SetRecognitionMode(PayCardsRecognizerMode flag) = 0;
    
    virtual void SetPathNumberRecognitionModel(const string& path) = 0;
    virtual void SetPathNumberRecognitionStruct(const string& path) = 0;
    
    virtual void SetPathNumberLocalizationXModel(const string& path) = 0;
    virtual void SetPathNumberLocalizationXStruct(const string& path) = 0;
    
    virtual void SetPathNumberLocalizationYModel(const string& path) = 0;
    virtual void SetPathNumberLocalizationYStruct(const string& path) = 0;
    virtual void SetDelegate(const shared_ptr<IRecognitionCoreDelegate>& delegate) = 0;

};


#endif /* INumberRecognizer_h */
