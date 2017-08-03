#ifndef HOUGH_H
#define HOUGH_H

#ifdef __cplusplus
#import <opencv2/opencv.hpp>
#endif

using namespace cv;

typedef struct LinePolar {
    float rho;
    float angle;
    bool is_null;
} LinePolar;

LinePolar llcv_hough(const Mat src_image, Mat dx, Mat dy, float rho, float theta, int threshold, float theta_min, float theta_max, bool vertical, float gradient_angle_threshold);

#endif
