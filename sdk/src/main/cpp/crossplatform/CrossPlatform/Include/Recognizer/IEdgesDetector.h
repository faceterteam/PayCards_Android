//
//  IEdgesDetector.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 11/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef IEdgesDetector_h
#define IEdgesDetector_h

#include "IBaseObj.h"
#include "IFrameStorage.h"
#include "IRecognitionCore.h"

using namespace std;

class IEdgesDetector : public IBaseObj
{
public:
    
    virtual ~IEdgesDetector() {}
    
public:
    
    virtual cv::Rect GetInternalWindowRect() const = 0;
    
    virtual const cv::Rect CalcWorkingArea(cv::Size frameSize, int captureAreaWidth, PayCardsRecognizerOrientation orientation) = 0;
    virtual const DetectedLineFlags DetectEdges(Mat& rawFrame,
                                                vector<ParametricLine>& edges, Mat& resultFrame) = 0;
};


#endif /* IEdgesDetector_h */
