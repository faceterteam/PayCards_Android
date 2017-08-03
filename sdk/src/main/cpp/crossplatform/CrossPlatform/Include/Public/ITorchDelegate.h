//
//  ITorchDelegate.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 20/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef ITorchDelegate_h
#define ITorchDelegate_h

#include <memory>

using namespace std;

class ITorchDelegate
{
public:
    
    virtual ~ITorchDelegate() {};
    
public:
    
    static bool GetInstance(shared_ptr<ITorchDelegate> &torchDelegate, void* platformDelegate = NULL);
    
    virtual void TorchStatusDidChange(bool status) = 0;
};

#endif /* ITorchDelegate_h */
