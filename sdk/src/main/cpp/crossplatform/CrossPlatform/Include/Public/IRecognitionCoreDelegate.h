//
//  IRecognitionCoreDelegate.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 13/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef IRecognitionCoreDelegate_h
#define IRecognitionCoreDelegate_h

#include <memory>
#include <opencv2/opencv.hpp>
#include "Enums.h"

using namespace std;

class IRecognitionResult;

class IRecognitionCoreDelegate
{
public:

    virtual ~IRecognitionCoreDelegate() {};
    
public:
    
    static bool GetInstance(shared_ptr<IRecognitionCoreDelegate> &recognitionDelegate, void* platformDelegate = NULL, void* recognizer = NULL);
    
    virtual void RecognitionDidFinish(const shared_ptr<IRecognitionResult>& result, PayCardsRecognizerMode resultFlags) = 0;
    virtual void CardImageDidExtract(cv::Mat cardImage) = 0;
    
//    virtual void NameRecognitionDidFinish(vector<pair<cv::Mat,std::string>> result) = 0;
};

#endif /* IRecognitionCoreDelegate_h */
