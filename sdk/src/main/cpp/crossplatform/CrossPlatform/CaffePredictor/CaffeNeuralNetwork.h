//
//  CaffeNeuralNetwork.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 08/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#ifndef __CardRecognizer__CaffeNeuralNetwork__
#define __CardRecognizer__CaffeNeuralNetwork__

#include "caffe/caffe.hpp"

#include "CaffeResultList.h"
#include "INeuralNetworkDatumList.h"
#include "INeuralNetworkResultList.h"
#include "INeuralNetwork.h"
#include "IServiceContainer.h"
#include "INeuralNetworkObjectFactory.h"

#include "caffe/caffe.hpp"

using namespace std;

class CCaffeNeuralNetwork : public INeuralNetwork
{
public:
    
    CCaffeNeuralNetwork(const shared_ptr<IServiceContainer>& serviceContainer, const string& name,
                        const string& pathToNetStructure, const string& pathToNetModel, const string& pathToMeanFile = "");
    
    virtual ~CCaffeNeuralNetwork();
    
    virtual const string GetName();
    
    virtual bool Predict(const shared_ptr<INeuralNetworkDatumList>& datumList, shared_ptr<INeuralNetworkResultList>& resultList);
    
    virtual bool IsDeployed() const;
    
private:
    
    void ProcessResult(const caffe::Blob<float>* prob, shared_ptr<INeuralNetworkResultList>& resultList);
    
private:
    
    shared_ptr<caffe::Net<float> >              _caffeNet;
    weak_ptr<IServiceContainer>                 _serviceContainer;
    weak_ptr<INeuralNetworkObjectFactory>       _objectFactory;

    string                                      _name;
    bool                                        _isDeployed;
};


#endif /* defined(__CardRecognizer__CaffeNeuralNetwork__) */
