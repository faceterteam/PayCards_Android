//
//  INeuralNetworkDatumList.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 09/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#ifndef __CardRecognizer__INeuralNetworkDatumList__
#define __CardRecognizer__INeuralNetworkDatumList__

#include "IBaseObj.h"
#include "INeuralNetworkDatum.h"

using namespace std;



class INeuralNetworkDatumList : public IBaseObj
{
public:
    
    virtual ~INeuralNetworkDatumList() {}
    
public:
    
    typedef vector<shared_ptr<INeuralNetworkDatum>>::iterator DatumIterator;
    typedef vector<shared_ptr<INeuralNetworkDatum>>::const_iterator ConstDatumIterator;
    
    virtual void PushBack(const shared_ptr<INeuralNetworkDatum>& datum) = 0;
    
    virtual int Size() const = 0;
    
    virtual void Clear() = 0;
    
};

#endif /* defined(__CardRecognizer__INeuralNetworkDatumList__) */
