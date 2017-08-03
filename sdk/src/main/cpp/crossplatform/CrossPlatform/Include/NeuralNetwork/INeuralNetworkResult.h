//
//  INeuralNetworkResult.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 09/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#ifndef __CardRecognizer__INeuralNetworkResult__
#define __CardRecognizer__INeuralNetworkResult__

#include "IBaseObj.h"

using namespace std;

struct ProbabilityData
{
    ProbabilityData(float maxProbability, const vector<pair<int, float>>& raw, int idx)
    : maxProbability(maxProbability), raw(raw), idx(idx) {}
    
    float maxProbability;
    vector<pair<int, float>> raw;
    int idx;
};


class INeuralNetworkResult : public IBaseObj
{
public:
    
    virtual ~INeuralNetworkResult() {}
    
public:
    
    virtual int GetMaxIndex() const = 0;

    virtual int GetMaxIndexInPossibleIndexes(const vector<int>& possibleIdxs) const = 0;
    
    virtual float GetMaxProbability() const = 0;
    
    virtual void GetSecondValue(int &secondValue, float &probabilityDiff) const = 0;
    
    virtual vector<pair<int, float>> GetRawResult() const = 0;
};


#endif /* defined(__CardRecognizer__INeuralNetworkResult__) */
