//
//  ITorchManager.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 20/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef ITorchManager_h
#define ITorchManager_h

#include "IBaseObj.h"

using namespace std;

class ITorchDelegate;

class ITorchManager : public IBaseObj
{
public:
    
    virtual ~ITorchManager() {}
    
public:
    virtual void SetDelegate(const shared_ptr<ITorchDelegate>& delegate) = 0;
    virtual void SetStatus(bool status) = 0;
    virtual bool GetStatus() const = 0;
    virtual void IncrementCounter() = 0;
    virtual void ResetCounter() = 0;
};


#endif /* ITorchManager_h */
