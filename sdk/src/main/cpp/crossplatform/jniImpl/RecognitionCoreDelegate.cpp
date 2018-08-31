//
//  WORecognitionCoreDelegate.m
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 13/01/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#include <time.h>
#include <android/log.h>
#include <unistd.h>
#include "RecognitionCoreDelegate.h"
#include "IRecognitionCore.h"
#include "INeuralNetworkResult.h"
#include "INeuralNetworkResultList.h"
#include "libyuv/convert_argb.h"

#define DEBUG_TAG "CardRecognizerJNI"

#ifdef NDEBUG
#define LOGI(...)
#else
#define LOGI(...) \
    ((void)__android_log_print(ANDROID_LOG_INFO, DEBUG_TAG, __VA_ARGS__))
#endif

static const std::vector<string> alphabet = {" ","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};

static int64_t getTimeNsec();

bool
IRecognitionCoreDelegate::GetInstance(shared_ptr<IRecognitionCoreDelegate> &recognitionDelegate,
                                      void *platformDelegate, void *recognizer) {
    recognitionDelegate = make_shared<CRecognitionCoreDelegate>(platformDelegate);
    return recognitionDelegate != 0;
}


CRecognitionCoreDelegate::CRecognitionCoreDelegate(void * env)
{
    JNIEnv *_env = (JNIEnv*)env;
    _env->GetJavaVM(&_jvm);

    jclass tmp = _env->FindClass("cards/pay/paycardsrecognizer/sdk/ndk/RecognitionCoreNdk");
    _clazz = (jclass)_env->NewGlobalRef(tmp);

    _clazzByteArray = (jclass)_env->NewGlobalRef(_env->FindClass("[B"));

    _method = _env->GetStaticMethodID(_clazz, "onRecognitionResultReceived",
                                      "("
                                              "ZZ"
                                              "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                                              "Landroid/graphics/Bitmap;"
                                              "IIII"
                                              ")V");

    _methodCardDidExtractCb = _env->GetStaticMethodID(_clazz, "onCardImageReceived", "(Landroid/graphics/Bitmap;)V");

    _clazzBitmap = (jclass)_env->NewGlobalRef(_env->FindClass("android/graphics/Bitmap"));
    _methodCreateBitmap = _env->GetStaticMethodID(_clazzBitmap, "createBitmap",
                                                  "([IIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");

    jclass bitmapConfig = _env->FindClass("android/graphics/Bitmap$Config");
    jfieldID rgb8888FieldID = _env->GetStaticFieldID(bitmapConfig, "ARGB_8888", "Landroid/graphics/Bitmap$Config;");
    _bitmapConfigArgb8888Obj = (jclass)_env->NewGlobalRef(_env->GetStaticObjectField(bitmapConfig, rgb8888FieldID));

    jfieldID rgb565FieldID = _env->GetStaticFieldID(bitmapConfig, "RGB_565", "Landroid/graphics/Bitmap$Config;");
    _bitmapConfigRgb565Obj = (jclass)_env->NewGlobalRef(_env->GetStaticObjectField(bitmapConfig, rgb565FieldID));

}

void CRecognitionCoreDelegate::RecognitionDidFinish(const shared_ptr<IRecognitionResult> &result, PayCardsRecognizerMode resultFlags)
{
    JNIEnv *jenv = nullptr;
    jstring jnumber = nullptr;
    jstring jname = nullptr;
    jstring jdate = nullptr;
    jstring jnameRaw = nullptr;
    jobject jCardImage = nullptr;
    jboolean jisFinal;
    jboolean jisFirst;

    int64_t startTime = getTimeNsec();

    _jvm->AttachCurrentThread(&jenv, NULL);

    try {
        // isFirst
        jisFirst = (jboolean) (resultFlags & PayCardsRecognizerModeNumber ? JNI_TRUE : JNI_FALSE);

        // isFinal
        jisFinal = (jboolean) (resultFlags & PayCardsRecognizerModeName ? JNI_TRUE : JNI_FALSE);

        // number
        shared_ptr<INeuralNetworkResultList> numberResult = result->GetNumberResult();

        if (numberResult != nullptr) {
            std::ostringstream numberStr;
            for (INeuralNetworkResultList::ResultIterator it = numberResult->Begin(); it != numberResult->End(); ++it) {
                shared_ptr<INeuralNetworkResult> result = *it;
                numberStr << result->GetMaxIndex();
            }
            jnumber = jenv->NewStringUTF(numberStr.str().c_str());
        }

        // date
        shared_ptr<INeuralNetworkResultList> dateResult = result->GetDateResult();
        if (dateResult != nullptr) {
            std::ostringstream dateStr;
            for (INeuralNetworkResultList::ResultIterator it = dateResult->Begin(); it != dateResult->End(); ++it) {
                shared_ptr<INeuralNetworkResult> result = *it;
                dateStr << result->GetMaxIndex();
            }
            jdate = jenv->NewStringUTF(dateStr.str().c_str());
        }

        // name
        std::string name = result->GetPostprocessedName();
        if (name.size() > 0) {
            LOGI("RecognitionDidFinish() name: %s", name.c_str());
            jname = jenv->NewStringUTF(name.c_str());
        }

        // RAW name
        shared_ptr<INeuralNetworkResultList> nameResult = result->GetNameResult();
        if (nameResult != nullptr) {
            std::ostringstream nameRawStr;
            for (INeuralNetworkResultList::ResultIterator it = nameResult->Begin(); it != nameResult->End(); ++it) {
                shared_ptr<INeuralNetworkResult> result = *it;
                nameRawStr << alphabet[result->GetMaxIndex()];
            }
            jnameRaw = jenv->NewStringUTF(nameRawStr.str().c_str());
        }

        // Card image
        {
            cv::Mat image = result->GetCardImage();

            if (!image.empty()) {
                jintArray jArgb = jenv->NewIntArray(image.total());
                unsigned *argb = (unsigned *) jenv->GetIntArrayElements(jArgb, NULL);

                libyuv::YToARGB(image.data, image.cols, (uint8_t *) argb, image.cols * 4, image.cols,
                                image.rows);

                jenv->ReleaseIntArrayElements(jArgb, (jint *) argb, 0);
                jCardImage = jenv->CallStaticObjectMethod(_clazzBitmap, _methodCreateBitmap,
                                                          jArgb,
                                                          image.cols, image.rows,
                                                          _bitmapConfigRgb565Obj);
            }
        }

        // Number Rect
        cv::Rect numberRect = result->GetNumberRect();

        jenv->CallStaticVoidMethod(_clazz, _method, jisFirst, jisFinal,
                                   jnumber, jdate, jname, jnameRaw,
                                   jCardImage,
                                   numberRect.x,
                                   numberRect.y,
                                   numberRect.width,
                                   numberRect.height);
    } catch (...) {
        jenv->ThrowNew(jenv->FindClass("java/lang/Exception"), "RecognitionDidFinish() error");
    }

    LOGI("RecognitionDidFinish() execution time: %u ms",
                        (unsigned)((getTimeNsec() - startTime) / 1000000));

    _jvm->DetachCurrentThread();
}

void CRecognitionCoreDelegate::CardImageDidExtract(cv::Mat cardImage)
{
    JNIEnv *jenv = NULL;
    _jvm->AttachCurrentThread(&jenv, NULL);
    jobject jBitmap = CreateJBitmap(jenv, cardImage, _bitmapConfigRgb565Obj);
    jenv->CallStaticVoidMethod(_clazz, _methodCardDidExtractCb, jBitmap, cardImage.rows, cardImage.cols);
    _jvm->DetachCurrentThread();
}

jobject CRecognitionCoreDelegate::CreateJBitmap(JNIEnv *jenv, cv::Mat rgbImage, jobject bitmapConfigObj) {
    int size = rgbImage.total();
    jintArray jArgb = jenv->NewIntArray(size);

    unsigned *argb = (unsigned *) jenv->GetIntArrayElements(jArgb, NULL);
    libyuv::RAWToARGB(rgbImage.data, rgbImage.cols * 3,(uint8_t *)argb, rgbImage.cols * 4, rgbImage.cols, rgbImage.rows);
    jenv->ReleaseIntArrayElements(jArgb, (jint *) argb, 0);
    /*
    cv::Mat tmp = cv::Mat (rgbImage.rows, rgbImage.cols, CV_8U, new cv:: Scalar(4));
    cvtColor(rgbImage, tmp, CV_RGB2BGRA, 4);
    jenv->SetIntArrayRegion(jArgb, 0, size, (const jint *) tmp.data);
*/
    jobject jBitmap = jenv->CallStaticObjectMethod(_clazzBitmap, _methodCreateBitmap,
                                                   jArgb,
                                                   rgbImage.cols, rgbImage.rows, bitmapConfigObj);

    return jBitmap;
}

static int64_t getTimeNsec() {
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (int64_t) now.tv_sec*1000000000LL + now.tv_nsec;
}
