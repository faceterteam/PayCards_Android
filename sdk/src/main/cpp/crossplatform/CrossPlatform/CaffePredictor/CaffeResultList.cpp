//
//  CaffeResultList.cpp
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 08/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#include "CaffeResultList.h"


CCaffeResultList::~CCaffeResultList()
{
}


void CCaffeResultList::PushBack(const shared_ptr<INeuralNetworkResult>& result)
{
    _resultList.push_back(result);
}

void CCaffeResultList::Clear()
{
    _resultList.clear();
}


int CCaffeResultList::Size() const
{
    return (int)_resultList.size();
}

shared_ptr<INeuralNetworkResult> CCaffeResultList::GetAtIndex(int idx)
{
    return _resultList.at(idx);
}

float CCaffeResultList::GetMeanConfidence() const
{
    float blockConfidence = 0.;
    for(auto it=this->Begin(); it != this->End(); ++it)
    {
        shared_ptr<INeuralNetworkResult> result = *it;
        blockConfidence += result->GetMaxProbability();
    }
    
    return blockConfidence;
}
