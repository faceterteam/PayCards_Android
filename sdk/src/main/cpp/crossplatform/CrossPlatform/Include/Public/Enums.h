//
//  Enums.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 16/03/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef Enums_h
#define Enums_h

typedef enum PayCardsRecognizerOrientation {
    PayCardsRecognizerOrientationUnknown = 0,
    PayCardsRecognizerOrientationPortrait = 1,
    PayCardsRecognizerOrientationPortraitUpsideDown = 2,
    PayCardsRecognizerOrientationLandscapeRight = 3,
    PayCardsRecognizerOrientationLandscapeLeft = 4
} PayCardsRecognizerOrientation;

typedef enum PayCardsRecognizerMode {
    PayCardsRecognizerModeNone = 0,
    PayCardsRecognizerModeNumber = 1,
    PayCardsRecognizerModeDate = 2,
    PayCardsRecognizerModeName = 4,
    PayCardsRecognizerModeGrabCardImage = 8
} PayCardsRecognizerMode;


#endif /* Enums_h */
