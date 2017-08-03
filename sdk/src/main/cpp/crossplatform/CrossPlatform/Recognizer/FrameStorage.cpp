//
//  FrameStorage.cpp
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 11/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#include <Eigen>

#include "FrameStorage.h"
#include "warp.h"
#include "Utils.h"


static const Size lowSize = Size(660,416);

CFrameStorage::CFrameStorage() : _grayOrientation(PayCardsRecognizerOrientationUnknown), _yuvOrientation(PayCardsRecognizerOrientationUnknown)
{
    int bufferHeightY = 1280;
    int bytesPerRowY = 720;
    
    int bufferHeightUV = 640;
    int bytesPerRowUV = 720;
    
    _bufferSizeY = bufferHeightY * bytesPerRowY;
    _bufferY = malloc(_bufferSizeY);
    
    _bufferSizeUV = bufferHeightUV * bytesPerRowUV;
    _bufferUV = malloc(_bufferSizeUV);
}

CFrameStorage::~CFrameStorage()
{
    _edges.clear();
    free(_bufferY);
    free(_bufferUV);
}

bool CFrameStorage::SetRawFrame(const Mat& rawFrame, const vector<ParametricLine>& edges, PayCardsRecognizerOrientation orientation)
{
    if (!NormalizeMatrix(rawFrame, edges, _frame)) return false;

    _grayOrientation = orientation;
    
    if (_grayOrientation != PayCardsRecognizerOrientationPortraitUpsideDown && _grayOrientation != PayCardsRecognizerOrientationPortrait) {
        
        CUtils::RotateMatrix90n(_frame, _frame, 90);
    }

    cv::resize(_frame, _frame, lowSize, CV_INTER_CUBIC);

    return true;
}

bool CFrameStorage::NormalizeMatrix(const Mat& matrix, const vector<ParametricLine>& edges, Mat& resultMatrix)
{
    vector<cv::Point> corners = {cv::Point(0,0),cv::Point(0,0),cv::Point(0,0),cv::Point(0,0)};
    
    if (!FindIntersectCorners(corners, edges)) return false;
    
    NormalizeMatrix(matrix, resultMatrix, 0, corners);
    
    return true;

}

void CFrameStorage::SetRawY(const void* rawY, const void* rawUV, const vector<ParametricLine>& edges, PayCardsRecognizerOrientation orientation)
{
    memcpy(_bufferY, rawY, _bufferSizeY);
    memcpy(_bufferUV, rawUV, _bufferSizeUV);
    _edges.clear();
    _edges = edges;
    _yuvOrientation = orientation;
}

bool CFrameStorage::GetCurrentFrame(Mat& mat)
{
    mat = _frame;
    return true;
}

void CFrameStorage::PopFrame()
{
    _frame = Scalar(0,0,0);
}

const void* CFrameStorage::GetUVMat()
{
    return _bufferUV;
}

const void* CFrameStorage::GetYMat()
{
    return _bufferY;
}

const PayCardsRecognizerOrientation CFrameStorage::GetYUVOrientation()
{
    return _yuvOrientation;
}

const vector<ParametricLine> CFrameStorage::GetEdges()
{
    return _edges;
}

//////////////////////////////////////

// 0 - top-left
// 1 - bottom-left
// 2 - top-right
// 3 - bottom-right
bool CFrameStorage::FindIntersectCorners(vector<cv::Point>& corners, vector<ParametricLine> edges)
{
    
    bool intersectsTL = ParametricIntersect(edges[0], edges[2], &corners[0].x, &corners[0].y);
    bool intersectsBL = ParametricIntersect(edges[1], edges[2], &corners[1].x, &corners[1].y);
    bool intersectsTR = ParametricIntersect(edges[0], edges[3], &corners[2].x, &corners[2].y);
    bool intersectsBR = ParametricIntersect(edges[1], edges[3], &corners[3].x, &corners[3].y);
    
    bool allIntersect = intersectsTL && intersectsBL && intersectsTR && intersectsBR;
    
    return allIntersect;
}

void CFrameStorage::NormalizeMatrix(const Mat& src, Mat& dst, int intend, vector<cv::Point>& corner_points)
{
    vector<cv::Point> srcPoints;
    
    srcPoints.push_back(corner_points[0]);
    srcPoints.push_back(corner_points[2]);
    srcPoints.push_back(corner_points[1]);
    srcPoints.push_back(corner_points[3]);
    
    llcv_unwarp(src, srcPoints, cv::Rect(intend/2, intend/2, src.cols-intend, src.rows-intend), dst);
}

bool CFrameStorage::ParametricIntersect(const ParametricLine& line1, const ParametricLine& line2, int *x, int *y)
{
    if(IsParametricLineNone(line1) || IsParametricLineNone(line2)) {
        return false;
    }
    
    Eigen::Matrix2f t;
    Eigen::Vector2f r;
    t << cosf(line1.theta), sinf(line1.theta), cosf(line2.theta), sinf(line2.theta);
    r << line1.rho, line2.rho;
    
    if(t.determinant() < 1e-10) {
        return false;
    }
    
    Eigen::Vector2f intersection = t.inverse() * r;
    *x = cvRound(intersection(0));
    *y = cvRound(intersection(1));
    return true;
}

bool CFrameStorage::IsParametricLineNone(ParametricLine line)
{
    return ((line).theta == FLT_MAX);
}



