//
//  INeuralNetworkDatum.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 09/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#ifndef __CardRecognizer__INeuralNetworkDatum__
#define __CardRecognizer__INeuralNetworkDatum__

#include "IBaseObj.h"

using namespace std;

class INeuralNetworkDatum : public IBaseObj
{
public:
    
    virtual ~INeuralNetworkDatum() {}
    
public:
    
    virtual int GetChannels() const = 0;

    virtual int GetHeight() const = 0;
    
    virtual int GetWidth() const = 0;
};


#endif /* defined(__CardRecognizer__INeuralNetworkDatum__) */
