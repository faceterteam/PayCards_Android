#include <Eigen>

#include "warp.h"

void llcv_calc_persp_transform(float *matrixData, int matrixDataSize, bool rowMajor, const vector<cv::Point> sourcePoints, const vector<cv::Point> destPoints) {

  // Set up matrices a and b so we can solve for x from ax = b
  // See http://xenia.media.mit.edu/~cwren/interpolator/ for a
  // good explanation of the basic math behind this.

  typedef Eigen::Matrix<float, 8, 8> Matrix8x8;
  typedef Eigen::Matrix<float, 8, 1> Matrix8x1;

  Matrix8x8 a;
  Matrix8x1 b;

  for(int i = 0; i < 4; i++) {
    a(i, 0) = sourcePoints[i].x;
    a(i, 1) = sourcePoints[i].y;
    a(i, 2) = 1;
    a(i, 3) = 0;
    a(i, 4) = 0;
    a(i, 5) = 0;
    a(i, 6) = -sourcePoints[i].x * destPoints[i].x;
    a(i, 7) = -sourcePoints[i].y * destPoints[i].x;

    a(i + 4, 0) = 0;
    a(i + 4, 1) = 0;
    a(i + 4, 2) = 0;
    a(i + 4, 3) = sourcePoints[i].x;
    a(i + 4, 4) = sourcePoints[i].y;
    a(i + 4, 5) = 1;
    a(i + 4, 6) = -sourcePoints[i].x * destPoints[i].y;
    a(i + 4, 7) = -sourcePoints[i].y * destPoints[i].y;

    b(i, 0) = destPoints[i].x;
    b(i + 4, 0) = destPoints[i].y;
  }

  // Solving ax = b for x, we get the values needed for our perspective
  // matrix. Table of options on the eigen site at
  // /dox/TutorialLinearAlgebra.html#TutorialLinAlgBasicSolve
  //
  // We use householderQr because it places no restrictions on matrix A,
  // is moderately fast, and seems to be sufficiently accurate.
  //
  // partialPivLu() seems to work as well, but I am wary of it because I
  // am unsure of A is invertible. According to the documenation and basic
  // performance testing, they are both roughly equivalent in speed.
  //
  // - @burnto

  Matrix8x1 x = a.householderQr().solve(b);

  // Initialize matrixData
  for (int i = 0; i < matrixDataSize; i++) {
    matrixData[i] = 0.0f;
  }
  int matrixSize = (matrixDataSize >= 16) ? 4 : 3;

  // Initialize a 4x4 eigen matrix. We may not use the final
  // column/row, but that's ok.
  Eigen::Matrix4f perspMatrix = Eigen::Matrix4f::Zero();

  // Assign a, b, d, e, and i
  perspMatrix(0, 0) = x(0, 0); // a
  perspMatrix(0, 1) = x(1, 0); // b
  perspMatrix(1, 0) = x(3, 0); // d
  perspMatrix(1, 1) = x(4, 0); // e
  perspMatrix(2, 2) = 1.0f;    // i

  // For 4x4 matrix used for 3D transform, we want to assign
  // c, f, g, and h to the fourth col and row.
  // So we use an offset for thes values
  int o = matrixSize - 3; // 0 or 1
  perspMatrix(0, 2 + o) = x(2, 0); // c
  perspMatrix(1, 2 + o) = x(5, 0); // f
  perspMatrix(2 + o, 0) = x(6, 0); // g
  perspMatrix(2 + o, 1) = x(7, 0); // h
  perspMatrix(2 + o, 2 + o) = 1.0f; // i

  // Assign perspective matrix to our matrixData buffer,
  // swapping row versus column if needed, and taking care not to
  // overflow if user didn't provide a large enough matrixDataSize.
  for(int c = 0; c < matrixSize; c++) {
    for(int r = 0; r < matrixSize; r++) {
      int index = rowMajor ? (c + r * matrixSize) : (r + c * matrixSize);
      if (index < matrixDataSize) {
        matrixData[index] = perspMatrix(r, c);
      }
    }
  }
  // TODO - instead of copying final values into matrixData return array, do one of:
  // (a) assign directly into matrixData, or
  // (b) use Eigen::Mat so that assignment goes straight into underlying matrixData
}




void llcv_unwarp(const Mat& input, const vector<cv::Point>& source_points, const cv::Rect& to_rect, Mat& output)
{
    float matrix[16];
	vector<cv::Point> dest_points;
    dest_points.push_back(cv::Point(to_rect.x, to_rect.y));
    dest_points.push_back(cv::Point(to_rect.x + to_rect.width, to_rect.y));
    dest_points.push_back(cv::Point(to_rect.x, to_rect.y + to_rect.height));
    dest_points.push_back(cv::Point(to_rect.x + to_rect.width, to_rect.y + to_rect.height));
    
    // Calculate row-major matrix
    llcv_calc_persp_transform(matrix, 9, true, source_points, dest_points);
    Mat cv_persp_mat = Mat(3, 3, CV_32FC1);
    for (int r = 0; r < 3; r++) {
        float* ptr = cv_persp_mat.ptr<float>(r);
        for (int c = 0; c < 3; c++) {
            ptr[c] =  matrix[3 * r + c];
        }
    }
    
    warpPerspective(input, output, cv_persp_mat, input.size(), CV_INTER_LINEAR + CV_WARP_FILL_OUTLIERS);
}



