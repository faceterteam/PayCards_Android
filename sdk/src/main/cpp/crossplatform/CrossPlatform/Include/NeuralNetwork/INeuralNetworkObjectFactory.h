//
//  INeuralNetworkObjectFactory.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 09/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#ifndef __CardRecognizer__INeuralNetworkObjectFactory__
#define __CardRecognizer__INeuralNetworkObjectFactory__

#include "INeuralNetwork.h"
#include "INeuralNetworkDatum.h"
#include "INeuralNetworkDatumList.h"
#include "INeuralNetworkResult.h"
#include "INeuralNetworkResultList.h"


using namespace std;
using namespace cv;

class INeuralNetworkObjectFactory : public IBaseObj
{
public:
    
    virtual ~INeuralNetworkObjectFactory() {}
    
public:
    
    virtual shared_ptr<INeuralNetwork> CreateNeuralNetwork(const string& netName, const string& pathToNetStructure,
                                                           const string& pathToNetModel, const string& pathToMeanFile = "") = 0;

    virtual shared_ptr<INeuralNetworkDatum> CreateNeuralNetworkDatum(const Mat& imageMat, bool needToBeNormalized = true,
                                                                     bool isColor = false, const int height = 0, const int width = 0) = 0;
    
    virtual shared_ptr<INeuralNetworkDatumList> CreateNeuralNetworkDatumList() = 0;
    
    virtual shared_ptr<INeuralNetworkResult> CreateNeuralNetworkResult(const vector<pair<int,float>>& rawResult,
                                                                                    int maxIndex, float maxProbability) = 0;
    
    virtual shared_ptr<INeuralNetworkResultList> CreateNeuralNetworkResultList() = 0;
    
};


#endif /* defined(__CardRecognizer__INeuralNetworkObjectFactory__) */
