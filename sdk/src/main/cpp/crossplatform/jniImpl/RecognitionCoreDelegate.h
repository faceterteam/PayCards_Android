//
//  RecognitionCoreDelegate.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 13/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#include <memory>
#include <jni.h>

#include "IRecognitionCoreDelegate.h"
#include "IRecognitionResult.h"
#include "Public/Enums.h"

class CRecognitionCoreDelegate : public IRecognitionCoreDelegate/* , public enable_shared_from_this<IRecognitionCoreDelegate> */
{
public:
    
    CRecognitionCoreDelegate();
    ~CRecognitionCoreDelegate() {};
    
public:
    
    CRecognitionCoreDelegate( void* platformDelegate );

    void RecognitionDidFinish(const shared_ptr<IRecognitionResult>& result, PayCardsRecognizerMode resultFlags);
    void CardImageDidExtract(cv::Mat cardImage);

private:

    JavaVM* _jvm;
    jclass _clazz;
    jclass _clazzByteArray;
    jmethodID _method;
    jmethodID _methodCardDidExtractCb;

    jclass _clazzBitmap;
    jmethodID _methodCreateBitmap;
    jobject _bitmapConfigArgb8888Obj;
    jobject _bitmapConfigRgb565Obj;

    jobject CreateJBitmap(JNIEnv *jenv, cv::Mat rgbImage, jobject bitmapConfigObj);

};
