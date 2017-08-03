//
//  IServiceContainer.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 09/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#ifndef __CardRecognizer__IServiceContainer__
#define __CardRecognizer__IServiceContainer__

#include "IBaseObj.h"

using namespace std;

class IServiceContainer : public IBaseObj, public enable_shared_from_this<IServiceContainer>
{
public:
    
    virtual ~IServiceContainer() {}
    
public:
    
    virtual bool Initialize() = 0;
    
    virtual shared_ptr<IBaseObj> resolve( const type_info& service ) = 0;
    
    template <class T>
    shared_ptr<T> resolve() { return dynamic_pointer_cast<T> (resolve(typeid(T))); }
    
};

class CServiceContainer;

class IServiceContainerFactory
{
public:
    
    static bool CreateServiceContainer(shared_ptr<IServiceContainer>& serviceContainer);
};

#endif /* defined(__CardRecognizer__IServiceContainer__) */
