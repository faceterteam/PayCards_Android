//
//  TorchManager.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 20/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef TorchManager_h
#define TorchManager_h

#include "ITorchManager.h"
#include "ITorchDelegate.h"

class CTorchManager : public ITorchManager
{
    
public:
    CTorchManager();
    
    virtual ~CTorchManager();
    
public:
    virtual void SetDelegate(const shared_ptr<ITorchDelegate>& delegate);
    virtual void SetStatus(bool status);
    virtual bool GetStatus() const;
    virtual void IncrementCounter();
    virtual void ResetCounter();

private:
    
    shared_ptr<ITorchDelegate> _torchDelegate;
    bool _status;
    int _counter;
};

#endif /* TorchManager_h */
