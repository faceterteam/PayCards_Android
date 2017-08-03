// The source below is a (perhaps modified) copy from OpenCV. OpenCV's license header:

/*M///////////////////////////////////////////////////////////////////////////////////////
//
//  IMPORTANT: READ BEFORE DOWNLOADING, COPYING, INSTALLING OR USING.
//
//  By downloading, copying, installing or using the software you agree to this license.
//  If you do not agree to this license, do not download, install,
//  copy or use the software.
//
//
//                        Intel License Agreement
//                For Open Source Computer Vision Library
//
// Copyright (C) 2000, Intel Corporation, all rights reserved.
// Third party copyrights are property of their respective owners.
//
// Redistribution and use in source and binary forms, with or without modification,
// are permitted provided that the following conditions are met:
//
//   * Redistribution's of source code must retain the above copyright notice,
//     this list of conditions and the following disclaimer.
//
//   * Redistribution's in binary form must reproduce the above copyright notice,
//     this list of conditions and the following disclaimer in the documentation
//     and/or other materials provided with the distribution.
//
//   * The name of Intel Corporation may not be used to endorse or promote products
//     derived from this software without specific prior written permission.
//
// This software is provided by the copyright holders and contributors "as is" and
// any express or implied warranties, including, but not limited to, the implied
// warranties of merchantability and fitness for a particular purpose are disclaimed.
// In no event shall the Intel Corporation or contributors be liable for any direct,
// indirect, incidental, special, exemplary, or consequential damages
// (including, but not limited to, procurement of substitute goods or services;
// loss of use, data, or profits; or business interruption) however caused
// and on any theory of liability, whether in contract, strict liability,
// or tort (including negligence or otherwise) arising in any way out of
// the use of this software, even if advised of the possibility of such damage.
//
//M*/

//#include "compile.h"
//#if COMPILE_DMZ

#include "hough.h"
//#include "opencv2/core/core.hpp"

#define TO_RADIANS(in_degrees) (CV_PI * (in_degrees) / 180.0f)
#define dmz_likely(x) __builtin_expect(!!(x),1)
#define dmz_unlikely(x) __builtin_expect(!!(x),0)


LinePolar llcv_hough(const Mat img, Mat dx_mat, Mat dy_mat, float rho, float theta, int threshold, float theta_min, float theta_max, bool vertical, float gradient_angle_threshold) {
//    CvMat img_stub, *img = (CvMat*)src_image;
//    img = cvGetMat(img, &img_stub);

//    CvMat dx_stub, *dx_mat = (CvMat*)dx;
//    dx_mat = cvGetMat(dx_mat, &dx_stub);
//
//    CvMat dy_stub, *dy_mat = (CvMat*)dy;
//    dy_mat = cvGetMat(dy_mat, &dy_stub);

//    if(!CV_IS_MASK_ARR(img)) {
//      CV_Error(CV_StsBadArg, "The source image must be 8-bit, single-channel");
//    }
//
//    if(rho <= 0 || theta <= 0 || threshold <= 0) {
//      CV_Error(CV_StsOutOfRange, "rho, theta and threshold must be positive");
//    }
//
//    if(theta_max < theta_min + theta) {
//      CV_Error(CV_StsBadArg, "theta + theta_min (param1) must be <= theta_max (param2)");
//    }

    cv::AutoBuffer<int> _accum;
    cv::AutoBuffer<int> _tabSin, _tabCos;

    const uchar* image;
    int step, width, height;
    int numangle, numrho;
    float ang;
    int r, n;
    int i, j;
    float irho = 1 / rho;
    float scale;

//    CV_Assert( CV_IS_MAT(img) && CV_MAT_TYPE(img->type) == CV_8UC1 );

    image = img.data;
    step = (int)img.step;
    width = img.cols;
    height = img.rows;

    const uint8_t *dx_mat_ptr = (uint8_t *)(dx_mat.data);
    int dx_step = (int)dx_mat.step;
    const uint8_t *dy_mat_ptr = (uint8_t *)(dy_mat.data);
    int dy_step = (int)dy_mat.step;

    numangle = cvRound((theta_max - theta_min) / theta);
    numrho = cvRound(((width + height) * 2 + 1) / rho);

    _accum.allocate((numangle+2) * (numrho+2));
    _tabSin.allocate(numangle);
    _tabCos.allocate(numangle);
    int *accum = _accum;
    int *tabSin = _tabSin, *tabCos = _tabCos;
    
    memset(accum, 0, sizeof(accum[0]) * (numangle + 2) * (numrho + 2));

#define FIXED_POINT_EXPONENT 10
#define FIXED_POINT_MULTIPLIER (1 << FIXED_POINT_EXPONENT)

    for(ang = theta_min, n = 0; n < numangle; ang += theta, n++) {
        tabSin[n] = (int)floorf(FIXED_POINT_MULTIPLIER * sinf(ang) * irho);
        tabCos[n] = (int)floorf(FIXED_POINT_MULTIPLIER * cosf(ang) * irho);
    }

    float slope_bound_a, slope_bound_b;
    if(vertical) {
        slope_bound_a = tanf((float)TO_RADIANS(180 - gradient_angle_threshold));
        slope_bound_b = tanf((float)TO_RADIANS(180 + gradient_angle_threshold));
    } else {
        slope_bound_a = tanf((float)TO_RADIANS(90 - gradient_angle_threshold));
        slope_bound_b = tanf((float)TO_RADIANS(90 + gradient_angle_threshold));
    }

    // stage 1. fill accumulator
    for(i = 0; i < height; i++) {
        int16_t *dx_row_ptr = (int16_t *)(dx_mat_ptr + i * dx_step);
        int16_t *dy_row_ptr = (int16_t *)(dy_mat_ptr + i * dy_step);
        for(j = 0; j < width; j++) {
            if(image[i * step + j] != 0) {
                int16_t del_x = dx_row_ptr[j];
                int16_t del_y = dy_row_ptr[j];

                bool use_pixel = false;

                if(dmz_likely(del_x != 0)) { // avoid div by 0
                  float slope = (float)del_y / (float)del_x;
                  if(vertical) {
                    if(slope >= slope_bound_a && slope <= slope_bound_b) {
                      use_pixel = true;
                    }
                  } else {
                    if(slope >= slope_bound_a || slope <= slope_bound_b) {
                      use_pixel = true;
                    }
                  }
                } else {
                  use_pixel = !vertical;
                }

                if(use_pixel) {
                    for(n = 0; n < numangle; n++) {
                        r = (j * tabCos[n] + i * tabSin[n]) >> FIXED_POINT_EXPONENT;
                        r += (numrho - 1) / 2;
                        accum[(n+1) * (numrho+2) + r+1]++;
                    }
                }
            }
        }
    }

    // stage 2. find maximum
    // TODO: NEON implementation of max/argmax to use here
    int maxVal = 0;
    int maxBase = 0;
    for( r = 0; r < numrho; r++ ) {
        for( n = 0; n < numangle; n++ ) {
            int base = (n + 1) * (numrho + 2) + r + 1;
            int accumVal = accum[base];
            if(accumVal > maxVal) {
                maxVal = accumVal;
                maxBase = base;
            }
        }
    }


    // stage 3. if local maximum is above threshold, add it
    LinePolar line;
    line.rho = 0.0f;
    line.angle = 0.0f;
    line.is_null = true;

    if(maxVal > threshold) {
      scale = 1.0f / (numrho + 2);
      int idx = maxBase;
      int n = cvFloor(idx * scale) - 1;
      int r = idx - (n + 1) * (numrho + 2) - 1;
      line.rho = (r - (numrho - 1) * 0.5f) * rho;
      line.angle = n * theta + theta_min;
      line.is_null = false;
    }
    return line;
}

//#endif
