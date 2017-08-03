//
//  IFrameStorage.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 11/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef IFrameStorage_h
#define IFrameStorage_h

#include "IBaseObj.h"
#include "IRecognitionCore.h"

using namespace std;
using namespace cv;

typedef struct {
    float rho;
    float theta;
} ParametricLine;

inline ParametricLine ParametricLineNone()
{
    ParametricLine l;
    l.rho = FLT_MAX;
    l.theta = FLT_MAX;
    return l;
}

class IFrameStorage : public IBaseObj
{
public:
    
    virtual ~IFrameStorage() {}
    
public:

    virtual bool SetRawFrame(const Mat& rawFrame, const vector<ParametricLine>& edges, PayCardsRecognizerOrientation orientation) = 0;
    
    virtual bool NormalizeMatrix(const Mat& matrix, const vector<ParametricLine>& edges, Mat& resultMatrix) = 0;
    
    virtual void SetRawY(const void* rawY, const void* rawUV, const vector<ParametricLine>& edges, PayCardsRecognizerOrientation orientation) = 0;
    
    virtual bool GetCurrentFrame(Mat& mat) = 0;
    virtual void PopFrame() = 0;
    virtual const void* GetUVMat() = 0;
    virtual const void* GetYMat() = 0;
    virtual const vector<ParametricLine> GetEdges() = 0;
    virtual const PayCardsRecognizerOrientation GetYUVOrientation() = 0;
};


#endif /* IFrameStorage_h */
