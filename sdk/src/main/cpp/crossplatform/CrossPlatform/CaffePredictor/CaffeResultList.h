//
//  CaffeResultList.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 08/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#ifndef __CardRecognizer__CaffeResultList__
#define __CardRecognizer__CaffeResultList__

#include "INeuralNetworkResultList.h"

using namespace std;

class CCaffeResultList : public INeuralNetworkResultList
{
public:

    CCaffeResultList() {};
    virtual ~CCaffeResultList();
    
    virtual void PushBack(const shared_ptr<INeuralNetworkResult>& result);
    
    virtual int Size() const;
    
    virtual void Clear();
    
    virtual ResultIterator Begin() {return _resultList.begin();}
    
    virtual ConstResultIterator Begin() const {return _resultList.cbegin();}
    
    virtual ResultIterator End() {return _resultList.end();}
    
    virtual ConstResultIterator End() const {return _resultList.cend();};

    virtual shared_ptr<INeuralNetworkResult> GetAtIndex(int idx);
    
    virtual float GetMeanConfidence() const;
    
private:
    
    vector<shared_ptr<INeuralNetworkResult>> _resultList;
};


#endif /* defined(__CardRecognizer__CaffeResultList__) */
