//
//  CaffeNeuralNetwork.cpp
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 08/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#include "CaffeNeuralNetwork.h"
#include "CaffeDatumList.h"
#include "Utils.h"

CCaffeNeuralNetwork::CCaffeNeuralNetwork(const shared_ptr<IServiceContainer>& serviceContainer, const string& name,
                                         const string& pathToNetStructure, const string& pathToNetModel,
                                         const string& pathToMeanFile) :
                                        _serviceContainer(serviceContainer), _name(name), _isDeployed(false)
{
    if(auto serviceContainer = _serviceContainer.lock()) {
        _objectFactory = serviceContainer->resolve<INeuralNetworkObjectFactory>();
        
        caffe::NetParameter netparam;
        
        if (ReadProtoFromTextFile(pathToNetStructure, &netparam)) {
            // instantiate network with structure
            _caffeNet = shared_ptr<caffe::Net<float>>(new caffe::Net<float>(netparam));
            
            // get trained data from binary file
            caffe::NetParameter netparamData;
            
            if (ReadProtoFromBinaryFile(pathToNetModel, &netparamData)) {
                _caffeNet->CopyTrainedLayersFrom(netparamData);
                _isDeployed = true;
            }
        }
    }
}

CCaffeNeuralNetwork::~CCaffeNeuralNetwork() {}

bool CCaffeNeuralNetwork::IsDeployed() const
{
    return _isDeployed;
}

const string CCaffeNeuralNetwork::GetName()
{
    return _name;
}

bool CCaffeNeuralNetwork::Predict(const shared_ptr<INeuralNetworkDatumList>& datumList, shared_ptr<INeuralNetworkResultList>& resultList)
{
    shared_ptr<CCaffeDatumList> datumImplPtr = dynamic_pointer_cast<CCaffeDatumList>(datumList);
    
    vector<cv::Mat> blobs = datumImplPtr->GetBlobs();
    
    caffe::Blob<float> *inputBlob = _caffeNet->input_blobs()[0];

    float *blobData = inputBlob->mutable_cpu_data();
    
    int size = inputBlob->width()*inputBlob->height();
    
    for (auto it = begin(blobs); it < end(blobs); ++it) {
        cv::Mat blob = *it;
        
        memcpy(blobData, blob.data, sizeof(float)*size);
        blobData += size;
    }
    
    _caffeNet->ForwardPrefilled();

    const caffe::Blob<float> *outputData = _caffeNet->output_blobs()[0];
    
    ProcessResult(outputData, resultList);
    return true;
}

void CCaffeNeuralNetwork::ProcessResult(const caffe::Blob<float>* output, shared_ptr<INeuralNetworkResultList>& resultList)
{
    int batchSize = output->num();
    int singleSampleNeuronsCount = output->count()/batchSize;
    int count = 0;
    
    for (int n=0; n<batchSize; n++) {
        vector<pair<int, float>> rawResultPtr;
        
        float maxValue= 0;
        int   maxIndex= 0;
        for (int i=0; i<singleSampleNeuronsCount; i++) {
            float val= output->cpu_data()[count];
            if (val > maxValue)
            {
                maxValue = val;
                maxIndex = i;
            }
            
            rawResultPtr.push_back(pair<int, float>(i, val));
            
            count++;
        }
        
        if(auto objectFactory = _objectFactory.lock()) {
            shared_ptr<INeuralNetworkResult> result = objectFactory->CreateNeuralNetworkResult(rawResultPtr, maxIndex, maxValue);
            resultList->PushBack(result);
        }
    }
}

