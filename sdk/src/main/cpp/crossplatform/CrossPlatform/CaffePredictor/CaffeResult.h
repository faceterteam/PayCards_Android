//
//  CaffeResult.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 08/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#ifndef __CardRecognizer__CaffeResult__
#define __CardRecognizer__CaffeResult__

#include "INeuralNetworkResult.h"

#include "caffe/caffe.hpp"

using namespace std;

class CCaffeResult : public INeuralNetworkResult
{
public:
    
    CCaffeResult(const vector<pair<int,float>>& rawResult, int maxIndex, float maxProbability);
    virtual ~CCaffeResult() {};

    virtual int GetMaxIndex() const;

    virtual int GetMaxIndexInPossibleIndexes(const vector<int>& possibleIdxs) const;
    
    virtual float GetMaxProbability() const;
    
    virtual void GetSecondValue(int& secondValue, float& probabilityDiff) const;
    
    virtual vector<pair<int,float>> GetRawResult() const;
    
private:
    
    vector<pair<int,float>>     _rawResult;
    int                         _maxIndex;
    float                       _maxProbability;
};


#endif /* defined(__CardRecognizer__CaffeResult__) */
