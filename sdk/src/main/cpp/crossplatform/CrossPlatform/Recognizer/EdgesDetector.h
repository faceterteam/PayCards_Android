//
//  EdgesDetector.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 11/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef EdgesDetector_h
#define EdgesDetector_h

#include "IEdgesDetector.h"
#include "IFrameStorage.h"

class CEdgesDetector : public IEdgesDetector
{
    
public:
    CEdgesDetector(shared_ptr<IFrameStorage> frameStorage);
    
    virtual ~CEdgesDetector();
    
public:

    virtual Rect GetInternalWindowRect() const;
    
    virtual const Rect CalcWorkingArea(Size frameSize, int captureAreaWidth, PayCardsRecognizerOrientation orienation);
    virtual const DetectedLineFlags DetectEdges(Mat& rawFrame, vector<ParametricLine>& edges, Mat& resultFrame);
    
private:
    
    ParametricLine LineByShiftingOrigin(const ParametricLine& oldLine, int xOffset, int yOffset);
    bool BestLine(ParametricLine* line, Mat& mat, bool expectedVertical);
    
private:
    
    int _lineThickness;
    int _lineFlags; // DetectedLineFlags

    Rect _internalWindowRect;
    
    PayCardsRecognizerOrientation _orientation;
};

#endif /* EdgeDetector_h */
