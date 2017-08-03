//
//  INeuralNetwork.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 09/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#ifndef __CardRecognizer__INeuralNetwork__
#define __CardRecognizer__INeuralNetwork__

#include "IBaseObj.h"
#include "INeuralNetworkResultList.h"
#include "INeuralNetworkDatumList.h"

using namespace std;

class INeuralNetwork : public IBaseObj
{
public:
    
    virtual ~INeuralNetwork() {}
    
public:
    
    virtual const string GetName() = 0;
    
    virtual bool Predict(const shared_ptr<INeuralNetworkDatumList>& datumList, shared_ptr<INeuralNetworkResultList>& resultList) = 0;
    
    virtual bool IsDeployed() const = 0;
};

#endif /* defined(__CardRecognizer__INeuralNetwork__) */
