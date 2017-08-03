//
//  Utils.cpp
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 21/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#include "Utils.h"
#include <string>


static std::string pathToDocumentFolder;
static std::string pathToBundleFolder;

bool CUtils::ValidateROI(const cv::Mat& matrix, const cv::Rect& rect)
{
    return (rect & cv::Rect(0, 0, matrix.cols, matrix.rows)) == rect;
}

void CUtils::RotateMatrix90n(cv::Mat &src, cv::Mat &dst, int angle) {
    dst.create(src.size(), src.type());
    if(angle == 270 || angle == -90) {
        // Rotate clockwise 270 degrees
        cv::transpose(src, dst);
        cv::flip(dst, dst, 0);
    }
    else if(angle == 180 || angle == -180) {
        // Rotate clockwise 180 degrees
        cv::flip(src, dst, -1);
    }
    else if(angle == 90 || angle == -270) {
        // Rotate clockwise 90 degrees
        cv::transpose(src, dst);
        cv::flip(dst, dst, 1);
    }
    else if(angle == 360 || angle == 0) {
        if(src.data != dst.data) {
            src.copyTo(dst);
        }
    }
}

