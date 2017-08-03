//
//  IRecognitionResult.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 11/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef IRecognitionResult_h
#define IRecognitionResult_h

#include "IBaseObj.h"

class INeuralNetworkResultList;

using namespace std;

enum
{
    RecognitionStatusIdle      = (1 << 0),
    RecognitionStatusNumber    = (1 << 1),
    RecognitionStatusDate      = (1 << 2),
    RecognitionStatusName      = (1 << 3),
    RecognitionStatusCompleted      = (1 << 4),
};
typedef char RecognitionStatus;


class IRecognitionResult : public IBaseObj
{
public:
    
    virtual ~IRecognitionResult() {}
    
public:
    
    virtual void Reset() = 0;
    
    virtual shared_ptr<INeuralNetworkResultList> GetNumberResult() const = 0;
    virtual shared_ptr<INeuralNetworkResultList> GetDateResult() const = 0;
    virtual shared_ptr<INeuralNetworkResultList> GetNameResult() const = 0;
    virtual RecognitionStatus GetRecognitionStatus() const = 0;
    virtual cv::Mat GetCardImage() const = 0;
    virtual cv::Rect GetNumberRect() const = 0;
    virtual cv::Rect GetDateRect() const = 0;
//    virtual vector<cv::Mat> GetNumberSamples() const = 0;
//    virtual vector<cv::Mat> GetDateSamples() const = 0;
//    virtual vector<cv::Mat> GetNameSamples() const = 0;
    virtual std::string GetPostprocessedName() const = 0;
    
    virtual void SetNumberResult(shared_ptr<INeuralNetworkResultList> result) = 0;
    virtual void SetDateResult(shared_ptr<INeuralNetworkResultList> result) = 0;
    virtual void SetNameResult(shared_ptr<INeuralNetworkResultList> result) = 0;
    virtual void SetRecognitionStatus(RecognitionStatus status) = 0;
    virtual void SetCardImage(cv::Mat cardImage) = 0;
    virtual void SetNumberRect(cv::Rect panRect) = 0;
    virtual void SetDateRect(cv::Rect panRect) = 0;
    
//    virtual void SetDateSamples(vector<cv::Mat> samples) = 0;
//    virtual void SetNumberSamples(vector<cv::Mat> samples) = 0;
//    virtual void SetNameSamples(vector<cv::Mat> samples) = 0;
    virtual void SetPostprocessedName(std::string name) = 0;
};

#endif /* IRecognitionResult_h */
