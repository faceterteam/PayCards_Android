//
//  CaffeDatum.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 09/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#ifndef __CardRecognizer__CaffeDatum__
#define __CardRecognizer__CaffeDatum__

#include "INeuralNetworkDatum.h"
#include "CaffeDatumList.h"

#include "caffe/caffe.hpp"


using namespace std;
using namespace cv;
using namespace caffe;


class CCaffeDatum : public INeuralNetworkDatum
{
public:
    
    CCaffeDatum(const Mat& imageMat, bool needToBeNormalized = false, bool isColor = false,
                const int height = 0, const int width = 0);
    
    virtual ~CCaffeDatum() {};

public:
    
    virtual int GetChannels() const;
    
    virtual int GetHeight() const;
    
    virtual int GetWidth() const;
    
    friend class CCaffeDatumList;
    
private:
    
    cv::Mat GetBlob() const;
    
    void SetupParamsAndData(const Mat& imageMat, bool needToBeNormalized, bool isColor);

private:
    
    cv::Mat _blob;
};




#endif /* defined(__CardRecognizer__CaffeDatum__) */
