//
//  warp.h
//  See the file "LICENSE.md" for the full license governing this code.
//

#ifndef WARP_H_
#define WARP_H_

#ifdef __cplusplus
#import <opencv2/opencv.hpp>
#endif

using namespace cv;
using namespace std;

// unwarps input image, interpolating image such that src_points map to dst_rect coordinates.
// Image is written to output IplImage.
void llcv_unwarp(const Mat& input, const vector<cv::Point>& source_points, const cv::Rect& to_rect, Mat& output);

// Solves and writes perpsective matrix to the matrixData buffer. 
// If matrixDataSize >= 16, uses a 4x4 matrix. Otherwise a 3x3. 
// Specifying rowMajor true writes to the buffer in row major format.
void llcv_calc_persp_transform(float *matrixData, int matrixDataSize, bool rowMajor, const vector<cv::Point> sourcePoints, const vector<cv::Point> destPoints);


#endif /* WARP_H_ */
