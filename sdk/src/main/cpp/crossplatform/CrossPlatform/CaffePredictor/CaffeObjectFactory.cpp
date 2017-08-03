//
//  CaffeObjectFactory.cpp
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 09/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#include "CaffeObjectFactory.h"

#include "INeuralNetwork.h"
#include "INeuralNetworkDatum.h"
#include "INeuralNetworkDatumList.h"
#include "INeuralNetworkResultList.h"
#include "INeuralNetworkResult.h"

#include "CaffeNeuralNetwork.h"
#include "CaffeDatum.h"
#include "CaffeDatumList.h"
#include "CaffeResultList.h"
#include "CaffeResult.h"


CCaffeObjectFactory::CCaffeObjectFactory(const shared_ptr<IServiceContainer>& container) : _serviceContainer(container)
{
    if(auto serviceContainer = _serviceContainer.lock()) {
        _objectFactory = serviceContainer->resolve<IObjectFactory>();
    }
}

CCaffeObjectFactory::~CCaffeObjectFactory()
{
}

shared_ptr<INeuralNetwork> CCaffeObjectFactory::CreateNeuralNetwork(const string& netName, const string& pathToNetStructure, const string& pathToNetModel, const string& pathToMeanFile)
{
    shared_ptr<INeuralNetwork> neuralNetwork;
    
    if(auto objectFactory = _objectFactory.lock()) {
        
        if(auto serviceContainer = _serviceContainer.lock()) {
            neuralNetwork = dynamic_pointer_cast<INeuralNetwork>(objectFactory->CreateObject<CCaffeNeuralNetwork>(serviceContainer, netName, pathToNetStructure, pathToNetModel, pathToMeanFile));
        }
    }
    
    return neuralNetwork;
}

shared_ptr<INeuralNetworkDatum> CCaffeObjectFactory::CreateNeuralNetworkDatum(const Mat& imageMat,
                                                                              bool needToBeNormalized, bool isColor/* = false*/,
                                                                                const int height/* = 0*/, const int width/* = 0*/)
{
    shared_ptr<INeuralNetworkDatum> datum;
    if(auto objectFactory = _objectFactory.lock()) {
        datum = dynamic_pointer_cast<INeuralNetworkDatum>(objectFactory->CreateObject<CCaffeDatum>(imageMat, needToBeNormalized, isColor, height, width));
    }
    
    return datum;
}

shared_ptr<INeuralNetworkDatumList> CCaffeObjectFactory::CreateNeuralNetworkDatumList()
{
    shared_ptr<INeuralNetworkDatumList> datumList;
    if(auto objectFactory = _objectFactory.lock()) {
        datumList = dynamic_pointer_cast<INeuralNetworkDatumList>(objectFactory->CreateObject<CCaffeDatumList>());
    }
    return datumList;
}

shared_ptr<INeuralNetworkResult> CCaffeObjectFactory::CreateNeuralNetworkResult(const vector<pair<int,float>>& rawResult,
                                                                                                    int maxIndex, float maxProbability)
{
    shared_ptr<INeuralNetworkResult> result;
    if(auto objectFactory = _objectFactory.lock()) {
        result = dynamic_pointer_cast<INeuralNetworkResult>(objectFactory->CreateObject<CCaffeResult>(rawResult, maxIndex, maxProbability));
    }
    
    return result;
}

shared_ptr<INeuralNetworkResultList> CCaffeObjectFactory::CreateNeuralNetworkResultList()
{
    shared_ptr<INeuralNetworkResultList> resultList;
    if(auto objectFactory = _objectFactory.lock()) {
        resultList = dynamic_pointer_cast<INeuralNetworkResultList>(objectFactory->CreateObject<CCaffeResultList>());
    }
    
    return resultList;
}
