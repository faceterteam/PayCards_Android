//
//  TorchDelegate.m
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 28/10/15.
//  Copyright © 2015 Vladimir Tchernitski. All rights reserved.
//

#include "ITorchDelegate.h"
#include "TorchDelegate.h"

bool ITorchDelegate::GetInstance(shared_ptr<ITorchDelegate> &torchDelegate, void* platformDelegate/* = NULL*/)
{
    torchDelegate = make_shared<CTorchDelegate>(platformDelegate);
    return torchDelegate != 0;
}

CTorchDelegate::CTorchDelegate(void *env)
{
    JNIEnv *jenv = (JNIEnv*)env;
    jenv->GetJavaVM(&_jvm);

    jclass tmp = jenv->FindClass("cards/pay/paycardsrecognizer/sdk/ndk/RecognitionCoreNdk");
    _clazz = (jclass)jenv->NewGlobalRef(tmp);

    _method = jenv->GetStaticMethodID(_clazz, "onTorchStatusChanged", "(Z)V");
}

void CTorchDelegate::TorchStatusDidChange(bool status)
{
    JNIEnv* jenv;
    int is_self_attached = 0;
    if (_jvm->GetEnv(reinterpret_cast<void**>(&jenv), JNI_VERSION_1_6) != JNI_OK) {
        _jvm->AttachCurrentThread(&jenv, NULL);
        _jvm->GetEnv(reinterpret_cast<void**>(&jenv), JNI_VERSION_1_6);
        is_self_attached = 1;
    }

    jenv->CallStaticVoidMethod(_clazz, _method, status );

    if (is_self_attached) _jvm->DetachCurrentThread();
}

