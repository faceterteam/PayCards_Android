//
//  CaffeDatum.cpp
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 09/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#include "CaffeDatum.h"

CCaffeDatum::CCaffeDatum(const Mat& imageMat, bool needToBeNormalized, bool isColor, const int height, const int width)
{
    if (!imageMat.data) {
        LOG(ERROR) << "Empty image matrix ";
    }
    else {
        SetupParamsAndData(imageMat, needToBeNormalized, isColor);
    }
}

void CCaffeDatum::SetupParamsAndData(const Mat& imageMat, bool needToBeNormalized, bool isColor)
{
    Mat imgFloat;
    imageMat.convertTo(imgFloat, CV_32FC1);
    imgFloat /= 255.0;

    _blob = imgFloat;
}


int CCaffeDatum::GetChannels() const
{
    return _blob.channels();
}

int CCaffeDatum::GetHeight() const
{
    return _blob.rows;
}

int CCaffeDatum::GetWidth() const
{
    return _blob.cols;
}

cv::Mat CCaffeDatum::GetBlob() const
{
    return _blob;
}
