//
//  IObjectFactory.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 09/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#ifndef __CardRecognizer__IObjectFactory__
#define __CardRecognizer__IObjectFactory__

#include "IBaseObj.h"

using namespace std;

class IObjectFactory : public IBaseObj
{
public:
    
    virtual ~IObjectFactory() {};
    
public:
    
    template<typename T, typename ... ARGS>
    shared_ptr<T> CreateObject(ARGS&&... args)
    {
        return shared_ptr<T>(new T { forward<ARGS>(args)... });
    }
    
};

#endif /* defined(__CardRecognizer__IObjectFactory__) */
