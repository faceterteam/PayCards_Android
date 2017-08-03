//
//  WOTorchDelegate.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 28/10/15.
//  Copyright © 2015 Vladimir Tchernitski. All rights reserved.
//

#include <memory>
#include <jni.h>

#include "ITorchDelegate.h"

class CTorchDelegate : public ITorchDelegate/* , public enable_shared_from_this<ITorchDelegate> */
{
public:
    
    CTorchDelegate();
    ~CTorchDelegate() {};
    
public:
    
    CTorchDelegate( void* platformDelegate);
    
    void TorchStatusDidChange(bool flag);

private:

    JavaVM* _jvm;
    jclass _clazz;
    jmethodID _method;
};
