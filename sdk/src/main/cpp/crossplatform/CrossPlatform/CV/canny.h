#ifndef CANNY_H
#define CANNY_H

#ifdef __cplusplus
#import <opencv2/opencv.hpp>
#endif

using namespace cv;

void llcv_adaptive_canny7_precomputed_sobel(Mat src, Mat dst, Mat dx, Mat dy);

#endif
