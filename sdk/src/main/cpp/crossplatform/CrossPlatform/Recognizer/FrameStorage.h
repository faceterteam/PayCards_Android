//
//  FrameStorage.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 11/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef FrameStorage_h
#define FrameStorage_h

#include "IFrameStorage.h"

class CFrameStorage : public IFrameStorage
{
    
public:
    CFrameStorage();
    
    virtual ~CFrameStorage();
    
public:
    
    virtual bool SetRawFrame(const Mat& rawFrame, const vector<ParametricLine>& edges, PayCardsRecognizerOrientation orientation);
    
    virtual bool NormalizeMatrix(const Mat& matrix, const vector<ParametricLine>& edges, Mat& resultMatrix);
    
    virtual void SetRawY(const void* rawY, const void* rawUV, const vector<ParametricLine>& edges, PayCardsRecognizerOrientation orientation);
    
    virtual bool GetCurrentFrame(Mat& mat);
    virtual void PopFrame();
    virtual const void* GetUVMat();
    virtual const void* GetYMat();
    virtual const vector<ParametricLine> GetEdges();
    virtual const PayCardsRecognizerOrientation GetYUVOrientation();

private:
    
    bool FindIntersectCorners(vector<cv::Point>& corners, vector<ParametricLine> edges);
    void NormalizeMatrix(const Mat& src, Mat& dst, int intend, vector<cv::Point>& corner_points);
    bool IsParametricLineNone(ParametricLine line);
    bool ParametricIntersect(const ParametricLine& line1, const ParametricLine& line2, int *x, int *y);
    
private:
    
    Mat _frame;
    vector<ParametricLine> _edges;
    
    void *_bufferY;
    size_t _bufferSizeY;
    
    void *_bufferUV;
    size_t _bufferSizeUV;
    
    PayCardsRecognizerOrientation _grayOrientation;
    PayCardsRecognizerOrientation _yuvOrientation;
};

#endif /* FrameStorage_hpp */
