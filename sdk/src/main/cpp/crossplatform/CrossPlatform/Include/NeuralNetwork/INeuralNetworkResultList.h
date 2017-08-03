//
//  INeuralNetworkResultList.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 09/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#ifndef __CardRecognizer__INeuralNetworkResultList__
#define __CardRecognizer__INeuralNetworkResultList__

#include "IBaseObj.h"
#include "INeuralNetworkResult.h"

using namespace std;

class INeuralNetworkResultList : public IBaseObj
{
public:
    
    virtual ~INeuralNetworkResultList() {}
    
public:

    typedef vector<shared_ptr<INeuralNetworkResult>>::iterator ResultIterator;
    typedef vector<shared_ptr<INeuralNetworkResult>>::const_iterator ConstResultIterator;
    
    virtual void PushBack(const shared_ptr<INeuralNetworkResult>& result) = 0;
    
    virtual int Size() const = 0;
    
    virtual void Clear() = 0;
    
    virtual ResultIterator Begin() = 0;
    
    virtual ConstResultIterator Begin() const = 0;
    
    virtual ResultIterator End() = 0;
    
    virtual ConstResultIterator End() const = 0;

    virtual shared_ptr<INeuralNetworkResult> GetAtIndex(int idx) = 0;
    
    virtual float GetMeanConfidence() const = 0;
    
};


#endif /* defined(__CardRecognizer__INeuralNetworkResultList__) */
