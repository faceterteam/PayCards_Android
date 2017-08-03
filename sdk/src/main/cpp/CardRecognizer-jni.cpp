#include <memory>
#include <string>
#include <jni.h>
#include <android/log.h>
#include "CrossPlatform/Include/Public/IRecognitionCore.h"
#include "CrossPlatform/Include/Public/IRecognitionCoreDelegate.h"
#include "CrossPlatform/Include/Public/ITorchDelegate.h"
#include "CrossPlatform/Recognizer/RecognitionCore.h"
#include "libyuv/rotate.h"
#include "libyuv/convert.h"

#define DEBUG_TAG "CardRecognizerJNI"

#ifdef NDEBUG
#define LOGI(...)
#else
#define LOGI(...) \
    ((void)__android_log_print(ANDROID_LOG_INFO, DEBUG_TAG, __VA_ARGS__))
#endif

#define PROCESS_FRAME_WIDTH 720
#define PROCESS_FRAME_HEIGHT 1280

extern "C" {
    static shared_ptr<IRecognitionCore> g_core = nullptr;

    static uint8_t frame_tmp_y[PROCESS_FRAME_WIDTH * PROCESS_FRAME_HEIGHT];
    static uint8_t frame_tmp_uv[PROCESS_FRAME_WIDTH * PROCESS_FRAME_HEIGHT / 2];

    static jboolean GetJStringContent(JNIEnv *AEnv, jstring AStr, std::string &ARes) {
        if (!AStr) {
            ARes.clear();
            AEnv->ThrowNew(AEnv->FindClass("java/lang/NullPointerException"), "ProcessFrame() error");
            return JNI_FALSE;
        }

        const char *s = AEnv->GetStringUTFChars(AStr,NULL);
        ARes=s;
        AEnv->ReleaseStringUTFChars(AStr,s);
        return JNI_TRUE;
    }

    static void setRect(JNIEnv *env, const Rect cvRect, jobject jRectDst)
    {
        jclass rectClass = env->GetObjectClass(jRectDst);
        jfieldID leftFieldID = env->GetFieldID(rectClass, "left", "I");
        jfieldID topFieldID = env->GetFieldID(rectClass, "top", "I");
        jfieldID rightFieldID = env->GetFieldID(rectClass, "right", "I");
        jfieldID bottomFieldID = env->GetFieldID(rectClass, "bottom", "I");

        env->SetIntField(jRectDst, leftFieldID, cvRect.x);
        env->SetIntField(jRectDst, topFieldID, cvRect.y);
        env->SetIntField(jRectDst, rightFieldID, cvRect.x + cvRect.height);
        env->SetIntField(jRectDst, bottomFieldID, cvRect.y + cvRect.width);
    }

    static inline PayCardsRecognizerOrientation getFromJOrientation(int jWorkAreaOrientation) {
        switch (jWorkAreaOrientation) {
            case 1 : return PayCardsRecognizerOrientationPortrait; // WORK_AREA_ORIENTATION_PORTAIT
            case 2 : return PayCardsRecognizerOrientationPortraitUpsideDown; // WORK_AREA_ORIENTATION_PORTAIT_UPSIDE_DOWN
            case 3 : return PayCardsRecognizerOrientationLandscapeRight; // WORK_AREA_ORIENTATION_LANDSCAPE_RIGHT
            case 4 : return PayCardsRecognizerOrientationLandscapeLeft; // WORK_AREA_ORIENTATION_LANDSCAPE_LEFT
            default: return PayCardsRecognizerOrientationUnknown;
        }
    }

    static inline const char *woOrientationName(PayCardsRecognizerOrientation v) {
        switch (v) {
            case PayCardsRecognizerOrientationPortrait: return "Portait";
            case PayCardsRecognizerOrientationPortraitUpsideDown: return "Portait Upside-down";
            case PayCardsRecognizerOrientationLandscapeRight: return "Landscape right";
            case PayCardsRecognizerOrientationLandscapeLeft: return "Landscape left";
            case PayCardsRecognizerOrientationUnknown:
            default:
                return "Unknown";
        }
    }

    JNIEXPORT void JNICALL
    Java_cards_pay_paycardsrecognizer_sdk_ndk_RecognitionCoreNdk_nativeSetDataPath(JNIEnv *env, jobject instance,
                                                                   jstring path_) {
        std::string stdPath;

        if (!GetJStringContent(env, path_, stdPath)) return;

        stdPath += "/";

        g_core->SetPathNumberRecognitionStruct(stdPath + "NumberRecognition/NumberRecognition.prototxt");
        g_core->SetPathNumberRecognitionModel(stdPath + "NumberRecognition/NumberRecognition.caffemodel");

        g_core->SetPathNumberLocalizationXModel(stdPath + "NumberLocalization/loc_x.caffemodel");
        g_core->SetPathNumberLocalizationXStruct(stdPath + "NumberLocalization/loc_x.prototxt");
        g_core->SetPathNumberLocalizationYModel(stdPath + "NumberLocalization/loc_y.caffemodel");
        g_core->SetPathNumberLocalizationYStruct(stdPath + "NumberLocalization/loc_y.prototxt");

        g_core->SetPathDateRecognitionModel(stdPath + "DateRecognition/DateRecognition.caffemodel");
        g_core->SetPathDateRecognitionStruct(stdPath + "DateRecognition/DateRecognition.prototxt");
        g_core->SetPathDateLocalization0Model(stdPath + "DateLocalization/DateLocalizationL0.caffemodel");
        g_core->SetPathDateLocalization0Struct(stdPath + "DateLocalization/DateLocalizationL0.prototxt");
        g_core->SetPathDateLocalization1Model(stdPath + "DateLocalization/DateLocalizationL1.caffemodel");
        g_core->SetPathDateLocalization1Struct(stdPath + "DateLocalization/DateLocalizationL1.prototxt");
        g_core->SetPathDateLocalizationViola(stdPath + "DateLocalization/cascade_date.xml");

        g_core->SetPathNameLocalizationXModel(stdPath + "NameLocalization/NameLocalizationX.caffemodel");
        g_core->SetPathNameLocalizationXStruct(stdPath + "NameLocalization/NameLocalizationX.prototxt");
        g_core->SetPathNameYLocalizationViola(stdPath + "NameLocalization/cascade_name.xml");

        g_core->SetPathNameSpaceCharModel(stdPath + "NameRecognition/NameSpaceCharRecognition.caffemodel");
        g_core->SetPathNameSpaceCharStruct(stdPath + "NameRecognition/NameSpaceCharRecognition.prototxt");
        g_core->SetPathNameListTxt(stdPath + "NameRecognition/names.txt");
    }

    JNIEXPORT void JNICALL
    Java_cards_pay_paycardsrecognizer_sdk_ndk_RecognitionCoreNdk_nativeInit(JNIEnv *env, jclass type) {

        shared_ptr<IRecognitionCoreDelegate> coreDelegate;
        IRecognitionCoreDelegate::GetInstance(coreDelegate, env);

        shared_ptr<ITorchDelegate> torchDelegate;
        ITorchDelegate::GetInstance(torchDelegate, env);

        IRecognitionCore::GetInstance(g_core, coreDelegate, torchDelegate);

        g_core->SetRecognitionMode((PayCardsRecognizerMode) (PayCardsRecognizerModeNumber
                                                             | PayCardsRecognizerModeName
                                                             | PayCardsRecognizerModeDate));
    }

    JNIEXPORT void JNICALL
    Java_cards_pay_paycardsrecognizer_sdk_ndk_RecognitionCoreNdk_nativeSetRecognitionMode(JNIEnv *env,
                                                                                      jobject instance,
                                                                                      jint recognitionMode) {
        g_core->SetRecognitionMode((PayCardsRecognizerMode)recognitionMode);
    }

    JNIEXPORT void JNICALL
    Java_cards_pay_paycardsrecognizer_sdk_ndk_RecognitionCoreNdk_nativeDeploy(JNIEnv *env,
                                                                       jobject instance) {
        g_core->Deploy();
    }

    JNIEXPORT void JNICALL
    Java_cards_pay_paycardsrecognizer_sdk_ndk_RecognitionCoreNdk_nativeSetOrientation(JNIEnv *env,
                                                                            jobject instance,
                                                                            jint cardRectOrientation) {
        LOGI("nativeSetOrientation() orientation: %s", woOrientationName(getFromJOrientation(cardRectOrientation)));
        g_core->SetOrientation(getFromJOrientation(cardRectOrientation));
    }

    JNIEXPORT void JNICALL
    Java_cards_pay_paycardsrecognizer_sdk_ndk_RecognitionCoreNdk_nativeCalcWorkingArea(JNIEnv *env,
                                                                         jobject instance,
                                                                         jint frameWidth,
                                                                         jint frameHeight,
                                                                         jint captureAreaWidth,
                                                                         jobject dstRect) {
        LOGI("nativeCalcWorkingArea() size: %ux%u, width: %u", frameWidth, frameHeight, captureAreaWidth);
        Rect rect = g_core->CalcWorkingArea(cv::Size(frameWidth, frameHeight), captureAreaWidth);
        setRect(env, rect, dstRect);
    }

    JNIEXPORT void JNICALL
    Java_cards_pay_paycardsrecognizer_sdk_ndk_RecognitionCoreNdk_nativeDestroy(JNIEnv *env, jclass type) {
        g_core = nullptr;
    }

    JNIEXPORT void JNICALL
    Java_cards_pay_paycardsrecognizer_sdk_ndk_RecognitionCoreNdk_nativeResetResult(JNIEnv *env,
                                                                         jobject instance) {
        g_core->ResetResult();
    }

    JNIEXPORT void JNICALL
    Java_cards_pay_paycardsrecognizer_sdk_ndk_RecognitionCoreNdk_nativeSetIdle(JNIEnv *env, jobject instance,
                                                                     jboolean idle) {
        g_core->SetIdle(idle);

    }

    JNIEXPORT jboolean JNICALL
    Java_cards_pay_paycardsrecognizer_sdk_ndk_RecognitionCoreNdk_nativeIsIdle(JNIEnv *env,
                                                                    jobject instance) {
        return (jboolean) g_core->IsIdle();
    }

    JNIEXPORT void JNICALL
    Java_cards_pay_paycardsrecognizer_sdk_ndk_RecognitionCoreNdk_nativeSetTorchStatus(JNIEnv *env,
                                                                            jobject instance,
                                                                            jboolean isTurnedOn) {
        g_core->SetTorchStatus(isTurnedOn);
    }

    JNIEXPORT jint JNICALL
    Java_cards_pay_paycardsrecognizer_sdk_ndk_RecognitionCoreNdk_nativeProcessFrameYV12(JNIEnv *env,
                                                                              jobject instance,
                                                                              jint src_width,
                                                                              jint src_height,
                                                                              jint rotation,
                                                                              jbyteArray buffer_) {


        DetectedLineFlags lineFlags = DetectedLineNoneFlag;
        uint8_t *buffer = (uint8_t *)env->GetByteArrayElements(buffer_, NULL);

        size_t src_wh = (size_t) (src_width * src_height);

        size_t dst_width = PROCESS_FRAME_WIDTH;
        size_t dst_height = PROCESS_FRAME_HEIGHT;
        size_t dst_wh = dst_width * dst_height;

        // Rotate YV12, convert to Y'UV420p (swap U and V)
        libyuv::I420Rotate(buffer, src_width,
                           &buffer[src_wh], src_width / 2,
                           &buffer[src_wh + src_wh / 4], src_width / 2,
                           frame_tmp_y, dst_width,
                           &frame_tmp_uv[dst_wh / 4], dst_width / 2,
                           frame_tmp_uv, dst_width / 2,
                           src_width, src_height,
                           (libyuv::RotationMode) rotation);


        //Mat frameMatY = cv::Mat(1280, 720, CV_8UC1, frame_tmp_y);
        //imwrite("/sdcard/1.png", frameMatY);

        try {
            g_core->ProcessFrame(lineFlags,
                                 frame_tmp_y,
                                 frame_tmp_uv,
                                 dst_wh,
                                 dst_wh / 2
            );
        } catch (...) {
            env->ThrowNew(env->FindClass("java/lang/Exception"), "ProcessFrame() error");
        }

        env->ReleaseByteArrayElements(buffer_, (jbyte *) buffer, 0);

        return lineFlags;
    }
}
