//
//  EdgeDetector.cpp
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 11/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#include <Eigen>

#include "EdgesDetector.h"
#include "canny.h"
#include "hough.h"
#include "IFrameStorage.h"

#define kIndent             30 // indent between full frame and working window |-indent-|window|-indent-|

#define kCardRatio          1.586 // bank card width / height

#define kHoughGradientAngleThreshold 10
#define kHoughThresholdLengthDivisor 12 // larger value --> accept more lines as lines

#define kHorizontalAngle ((float)(CV_PI / 2.0f))
#define kVerticalAngle ((float)CV_PI)
#define kMaxAngleDeviationAllowed ((float)(5.0f * (CV_PI / 180.0f)))

CEdgesDetector::CEdgesDetector(shared_ptr<IFrameStorage> frameStorage) : _lineFlags(DetectedLineNoneFlag), _orientation(PayCardsRecognizerOrientationUnknown)
{
}

CEdgesDetector::~CEdgesDetector()
{
}

Rect CEdgesDetector::GetInternalWindowRect() const
{
    return _internalWindowRect;
}

const Rect CEdgesDetector::CalcWorkingArea(Size frameSize, int captureAreaWidth,
                                           PayCardsRecognizerOrientation orienation)
{
    _orientation = orienation;
    
    vector<cv::Rect> result;
    
    _lineThickness = captureAreaWidth;
    
    int height = frameSize.height - 2*kIndent;
    int width = (int)height/kCardRatio;
    
    
    cv::Rect windowRect;
    int x, y;
    
    if (orienation == PayCardsRecognizerOrientationPortrait || orienation == PayCardsRecognizerOrientationPortraitUpsideDown) {
        
        x = (frameSize.height - height)/2;
        y = (frameSize.width - width)/2;
        
        windowRect = cv::Rect(x,y,width,height);
    }
    else {
        x = (frameSize.height - width)/2;
        y = (frameSize.width - height)/2;
        
        windowRect = cv::Rect(x,y,height,width);
    }
    
    _internalWindowRect = cv::Rect(windowRect.x-_lineThickness/2, windowRect.y-_lineThickness/2,
                                   windowRect.height + _lineThickness, windowRect.width + _lineThickness);    
    return windowRect;
}

const DetectedLineFlags CEdgesDetector::DetectEdges(Mat& rawFrame, vector<ParametricLine>& edges, Mat& resultFrame)
{
    if (_orientation == PayCardsRecognizerOrientationUnknown)
        return DetectedLineNoneFlag;
    
    const Mat windowMat = rawFrame(_internalWindowRect);
    
    int lineThicknessX2 = _lineThickness*2;
    
    cv::Rect topRect = cv::Rect(_lineThickness, 0, _internalWindowRect.width-lineThicknessX2, _lineThickness);
    Mat topEdge = windowMat(topRect);
    
    cv::Rect bottomRect = cv::Rect(_lineThickness, _internalWindowRect.height-_lineThickness, _internalWindowRect.width-lineThicknessX2, _lineThickness);
    Mat bottomEdge = windowMat(bottomRect);
    
    cv::Rect leftRect = cv::Rect(0, _lineThickness, _lineThickness, _internalWindowRect.height-lineThicknessX2);
    Mat leftEdge = windowMat(leftRect);
    
    cv::Rect rightRect = cv::Rect(_internalWindowRect.width-_lineThickness, _lineThickness, _lineThickness, _internalWindowRect.height-lineThicknessX2);
    Mat rightEdge = windowMat(rightRect);
    
    _lineFlags = DetectedLineNoneFlag;
    
    ParametricLine topLine;
    if(BestLine(&topLine, topEdge, false)) {
        
        if (_orientation == PayCardsRecognizerOrientationPortrait || _orientation == PayCardsRecognizerOrientationPortraitUpsideDown) {
            _lineFlags |= DetectedLineTopFlag;
        }
        else {
            _lineFlags |= DetectedLineRightFlag;
        }
    }
    
    ParametricLine bottomLine;
    if(BestLine(&bottomLine, bottomEdge, false)) {
        
        if (_orientation == PayCardsRecognizerOrientationPortrait || _orientation == PayCardsRecognizerOrientationPortraitUpsideDown) {
            _lineFlags |= DetectedLineBottomFlag;
        }
        else {
            _lineFlags |= DetectedLineLeftFlag;
        }
    }
    
    ParametricLine leftLine;
    if(BestLine(&leftLine, leftEdge, true)) {
        
        if (_orientation == PayCardsRecognizerOrientationPortrait || _orientation == PayCardsRecognizerOrientationPortraitUpsideDown) {
            _lineFlags |= DetectedLineLeftFlag;
        }
        else {
            _lineFlags |= DetectedLineTopFlag;
        }
    }
    
    ParametricLine rightLine;
    if(BestLine(&rightLine, rightEdge, true)) {
        
        if (_orientation == PayCardsRecognizerOrientationPortrait || _orientation == PayCardsRecognizerOrientationPortraitUpsideDown) {
            _lineFlags |= DetectedLineRightFlag;
        }
        else {
            _lineFlags |= DetectedLineBottomFlag;
        }
    }
    
    if (_lineFlags&DetectedLineTopFlag &&  _lineFlags&DetectedLineBottomFlag
        && _lineFlags&DetectedLineLeftFlag && _lineFlags&DetectedLineRightFlag) {
        
        edges.clear();
        
        edges.push_back(LineByShiftingOrigin(topLine, topRect.x, topRect.y));
        edges.push_back(LineByShiftingOrigin(bottomLine, bottomRect.x, bottomRect.y));
        edges.push_back(LineByShiftingOrigin(leftLine, leftRect.x, leftRect.y));
        edges.push_back(LineByShiftingOrigin(rightLine, rightRect.x, rightRect.y));
        
        resultFrame = rawFrame(_internalWindowRect);
    }
    
    return (DetectedLineFlags)_lineFlags;
}

bool CEdgesDetector::BestLine(ParametricLine* line, Mat& mat, bool expectedVertical)
{
    Mat dx, dy;
    
    Mat dst = Mat(mat.rows, mat.cols, CV_8UC1);
    
    Sobel(mat, dx, CV_16S, 1, 0);
    Sobel(mat, dy, CV_16S, 0, 1);
    
    llcv_adaptive_canny7_precomputed_sobel(mat, dst, dx, dy);
    
    // Calculate the hough transform, throwing away edge components with the wrong gradient angles
    int hough_accumulator_threshold = MAX(dst.cols, dst.rows) / kHoughThresholdLengthDivisor;
    float base_angle = expectedVertical ? kVerticalAngle : kHorizontalAngle;
    float theta_min = base_angle - kMaxAngleDeviationAllowed;
    float theta_max = base_angle + kMaxAngleDeviationAllowed;
    
    LinePolar bestline = llcv_hough(dst,
                                    dx, dy,
                                    1, // rho resolution
                                    (float)CV_PI / 180.0f, // theta resolution
                                    hough_accumulator_threshold,
                                    theta_min,
                                    theta_max,
                                    expectedVertical,
                                    kHoughGradientAngleThreshold);
    
    *line = ParametricLineNone();
    
    if(!bestline.is_null) {
        line->rho = bestline.rho;
        line->theta = bestline.angle;
        
        return true;
    }
    
    return false;
}

ParametricLine CEdgesDetector::LineByShiftingOrigin(const ParametricLine& oldLine, int xOffset, int yOffset)
{
    ParametricLine newLine;
    newLine.theta = oldLine.theta;
    double offsetAngle = xOffset == 0 ? CV_PI / 2.0f : atan((float)yOffset / (float)xOffset);
    double deltaAngle = oldLine.theta - offsetAngle + CV_PI / 2.0f; // because we're working with the line *normal* to theta
    double offsetMagnitude = sqrt(xOffset * xOffset + yOffset * yOffset);
    double delta_rho = offsetMagnitude * cos(CV_PI / 2 - deltaAngle);
    newLine.rho = (float)(oldLine.rho + delta_rho);
    return newLine;
}


