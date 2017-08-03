//
//  CaffeObjectFactory.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 09/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#ifndef __CardRecognizer__CaffeObjectFactory__
#define __CardRecognizer__CaffeObjectFactory__

#include "INeuralNetworkObjectFactory.h"
#include "IServiceContainer.h"
#include "IObjectFactory.h"

using namespace std;

class CCaffeObjectFactory : public INeuralNetworkObjectFactory
{
public:
    
    CCaffeObjectFactory(const shared_ptr<IServiceContainer>& container);
    virtual ~CCaffeObjectFactory();
    
    virtual shared_ptr<INeuralNetwork> CreateNeuralNetwork(const string& netName, const string& pathToNetStructure, const string& pathToNetModel, const string& pathToMeanFile = "");
    
    virtual shared_ptr<INeuralNetworkDatum> CreateNeuralNetworkDatum(const Mat& imageMat, bool needToBeNormalized = true, bool isColor = false,
                                                                            const int height = 0, const int width = 0);
    
    virtual shared_ptr<INeuralNetworkDatumList> CreateNeuralNetworkDatumList();
    
    virtual shared_ptr<INeuralNetworkResult> CreateNeuralNetworkResult(const vector<pair<int,float>>& rawResult,
                                                                                    int maxIndex, float maxProbability);
    
    virtual shared_ptr<INeuralNetworkResultList> CreateNeuralNetworkResultList();

private:
    
    weak_ptr<IServiceContainer> _serviceContainer;
    weak_ptr<IObjectFactory> _objectFactory;
};


#endif /* defined(__CardRecognizer__CaffeObjectFactory__) */
