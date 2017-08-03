//
//  Utils.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 21/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef Utils_h
#define Utils_h

#include <opencv2/opencv.hpp>
#include <fstream>

class CUtils
{
    
public:
    CUtils() {};
    
    virtual ~CUtils() {};
    
public:
    
    static bool ValidateROI(const cv::Mat& matrix, const cv::Rect& rect);
    static void RotateMatrix90n(cv::Mat &src, cv::Mat &dst, int angle);
    
private:
    template <typename T>
    static std::string to_string(T value);

};


#endif /* Utils_h */
