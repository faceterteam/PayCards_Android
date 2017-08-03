//
//  TorchManager.cpp
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 20/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#include "TorchManager.h"

#define kNonTorchTries 10

CTorchManager::CTorchManager() : _status(false), _counter(0)
{
}

CTorchManager::~CTorchManager()
{
    SetStatus(false);
}

void CTorchManager::SetDelegate(const shared_ptr<ITorchDelegate>& delegate)
{
    _torchDelegate = delegate;
}

void CTorchManager::SetStatus(bool status)
{
    if (status == GetStatus()) return;
    
    _status = status;
    
    if (!status) ResetCounter();
    
    _torchDelegate->TorchStatusDidChange(_status);
}

bool CTorchManager::GetStatus() const
{
    return _status;
}

void CTorchManager::IncrementCounter()
{
    _counter++;
    
    if (_counter > kNonTorchTries) {
        SetStatus(true);
    }
}

void CTorchManager::ResetCounter()
{
    _counter = 0;
}
