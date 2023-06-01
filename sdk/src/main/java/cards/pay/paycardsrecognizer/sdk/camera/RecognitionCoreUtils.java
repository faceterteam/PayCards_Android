package cards.pay.paycardsrecognizer.sdk.camera;

import android.content.Context;

import androidx.annotation.RestrictTo;

import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionCore;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class RecognitionCoreUtils {

    private RecognitionCoreUtils() {
    }

    public static boolean isRecognitionCoreDeployRequired(Context context) {
        //noinspection RedundantIfStatement
        if (!CameraUtils.isCameraSupported(context)
                || !RecognitionAvailabilityChecker.isDeviceNewEnough(context)
                || RecognitionCore.isInitialized()) {
            return false;
        }
        return true;
    }

    public static void deployRecognitionCoreSync(Context context) {
        if (!isRecognitionCoreDeployRequired(context)) return;

        try {
            RecognitionCore.deploy(context);
        } catch (Throwable e) {
            // IGNORE
        }
    }
}
