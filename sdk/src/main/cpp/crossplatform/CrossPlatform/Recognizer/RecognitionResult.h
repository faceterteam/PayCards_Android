//
//  RecognitionResult.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 11/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef RecognitionResult_h
#define RecognitionResult_h

#include "IRecognitionResult.h"

class CRecognitionResult : public IRecognitionResult
{
    
public:
    CRecognitionResult();
    
    virtual ~CRecognitionResult();
    
public:
    
    virtual void Reset();
    
    virtual shared_ptr<INeuralNetworkResultList> GetNumberResult() const;
    virtual shared_ptr<INeuralNetworkResultList> GetDateResult() const;
    virtual shared_ptr<INeuralNetworkResultList> GetNameResult() const;
    virtual RecognitionStatus GetRecognitionStatus() const;
    virtual cv::Mat GetCardImage() const;
    virtual cv::Rect GetNumberRect() const;
    virtual cv::Rect GetDateRect() const;
    virtual std::string GetPostprocessedName() const;
    
    virtual void SetNumberResult(shared_ptr<INeuralNetworkResultList> result);
    virtual void SetDateResult(shared_ptr<INeuralNetworkResultList> result);
    virtual void SetNameResult(shared_ptr<INeuralNetworkResultList> result);
    virtual void SetRecognitionStatus(RecognitionStatus status);
    virtual void SetCardImage(cv::Mat cardImage);
    virtual void SetNumberRect(cv::Rect numberRect);
    virtual void SetDateRect(cv::Rect dateRect);
    virtual void SetPostprocessedName(std::string name);
        
private:
    
    shared_ptr<INeuralNetworkResultList> _numberResult;
    shared_ptr<INeuralNetworkResultList> _dateResult;
    shared_ptr<INeuralNetworkResultList> _nameResult;
    RecognitionStatus _recognitionStatus;
    cv::Mat _cardImage;
    cv::Rect _numberRect;
    cv::Rect _dateRect;

    std::string _postprocessedName;
};


#endif /* RecognitionResult_h */
