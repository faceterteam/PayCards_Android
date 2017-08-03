//
//  CaffeResult.cpp
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 08/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#include "CaffeResult.h"

CCaffeResult::CCaffeResult(const vector<pair<int,float>>& rawResult, int maxIndex, float maxProbability) :
_rawResult(rawResult), _maxIndex(maxIndex), _maxProbability(maxProbability)
{
}

int CCaffeResult::GetMaxIndex() const
{
    return _maxIndex;
}

float CCaffeResult::GetMaxProbability() const
{
    return _maxProbability;
}

int CCaffeResult::GetMaxIndexInPossibleIndexes(const vector<int>& possibleIdxs) const
{
    vector<pair<int,float>> res(_rawResult);
    
    sort(res.begin(), res.end(), [](const std::pair<int,float> &left, const std::pair<int,float> &right) {
        return left.second > right.second;
    });

    int result = -1;
    
    for(auto it = res.begin(); it < res.end(); ++it) {
        
        pair<int, float> pair = *it;
        
        int idx = pair.first;
        
        if (find(possibleIdxs.begin(), possibleIdxs.end(), idx) != possibleIdxs.end()) {
            result = idx;
            break;
        }
    }
    
    return result;
}

void CCaffeResult::GetSecondValue(int &secondValue, float &probabilityDiff) const
{
    vector<pair<int,float>> res(_rawResult);
    
    sort(res.begin(), res.end(), [](const std::pair<int,float> &left, const std::pair<int,float> &right) {
        return left.second > right.second;
    });
    secondValue = res.at(1).first;
    probabilityDiff = res.at(1).second;
}

vector<pair<int,float>> CCaffeResult::GetRawResult() const
{
    return _rawResult;
}
