//
//  CaffeDatumList.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 08/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#ifndef __CardRecognizer__CaffeDatumList__
#define __CardRecognizer__CaffeDatumList__

#include <vector>

#include "CaffeDatumList.h"
#include "INeuralNetworkDatumList.h"
#include "INeuralNetworkDatum.h"
#include "CaffeNeuralNetwork.h"

#include "caffe/caffe.hpp"

using namespace std;

class CCaffeDatumList : public INeuralNetworkDatumList
{
public:
    
    CCaffeDatumList() {};

    virtual ~CCaffeDatumList() {};
    
    virtual void PushBack(const shared_ptr<INeuralNetworkDatum>& datum);
    
    virtual int Size() const;
    
    virtual void Clear();
    
    friend class CCaffeNeuralNetwork;

private:
    vector<cv::Mat> GetBlobs() const;
    
private:    
    vector<cv::Mat> _blobs;
};


#endif /* defined(__CardRecognizer__CaffeDatumList__) */
