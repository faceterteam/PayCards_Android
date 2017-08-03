package cards.pay.paycardsrecognizer.sdk.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.RestrictTo;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.facebook.device.yearclass.YearClass;

import java.util.Locale;

import cards.pay.paycardsrecognizer.sdk.BuildConfig;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionCore;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class RecognitionAvailabilityChecker {

    private static final boolean DBG = BuildConfig.DEBUG;

    public static final String TAG = "CameraChecker";

    public RecognitionAvailabilityChecker() {
    }

    // Execute non-blocking tests
    public static Result doCheck(Context context) {
        return doCheckInternal(context).build();
    }

    public static Result doCheckBlocking(Context context) {
        RecognitionCheckResultBuilder builder = doCheckInternal(context);
        Result result = builder.build();
        if (!builder.build().isAdditionalCheckRequired()) {
            return result;
        }

        builder.isBlockingCheck(true);
        builder.recognitionCoreSupported(RecognitionCore.getInstance(context).isDeviceSupported());
        if (builder.recognitionCoreSupported == Result.STATUS_FAILED) {
            return builder.build();
        }

        builder.isCameraSupported(CameraUtils.isCameraSupportedBlocking());

        return builder.build();
    }

    private static RecognitionCheckResultBuilder doCheckInternal(Context context) {
        RecognitionCheckResultBuilder builder = new RecognitionCheckResultBuilder()
                .isBlockingCheck(false)
                .isDeviceNewEnough(isDeviceNewEnough(context))
                .hasCamera(isDeviceHasCamera(context))
                .hasCameraPermission(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                ;

        try {
            builder.isCameraSupported(CameraUtils.isCameraSupported());
        } catch (BlockingOperationException e) {
            // IGNORE
        }

        if (RecognitionCore.isInitialized()) {
            builder.recognitionCoreSupported(RecognitionCore.getInstance(context).isDeviceSupported());
        }

        return builder;
    }

    public static boolean isDeviceNewEnough(Context context) {
        int year = YearClass.get(context);
        if (DBG) Log.d(TAG, "Device year is: " + year);
        return year >= 2011;
    }

    public static boolean isDeviceHasCamera(Context context) {
        PackageManager pm = context.getPackageManager();
        boolean hasCameraFeature = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
        boolean hasAutofocus = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
        return hasCameraFeature/* && hasAutofocus*/;
    }

    public static class Result {

        public static final int STATUS_NOT_CHECKED = 0;

        public static final int STATUS_PASSED = 1;

        public static final int STATUS_FAILED = -1;

        public final boolean isBlockingCheck;

        public final int isDeviceNewEnough;

        public final int recognitionCoreSupported;

        public final int hasCamera;

        public final int hasCameraPermission;

        public final int isCameraSupported;

        Result(boolean isBlockingCheck,
               int isDeviceNewEnough,
               int recognitionCoreSupported,
               int hasCamera,
               int hasCameraPermission,
               int isCameraSupported) {
            this.isBlockingCheck = isBlockingCheck;
            this.isDeviceNewEnough = isDeviceNewEnough;
            this.recognitionCoreSupported = recognitionCoreSupported;
            this.hasCamera = hasCamera;
            this.hasCameraPermission = hasCameraPermission;
            this.isCameraSupported = isCameraSupported;
        }

        private boolean isFailedNonBlocking() {
            // some of checked tests are failed
            return (isDeviceNewEnough == STATUS_FAILED)
                    || (recognitionCoreSupported == STATUS_FAILED)
                    || (hasCamera == STATUS_FAILED)
                    || (hasCameraPermission == STATUS_FAILED)
                    || (isCameraSupported == STATUS_FAILED);
        }

        public boolean isFailed() {
            if (isBlockingCheck) {
                return !isPassed();
            } else {
                return isFailedNonBlocking();
            }
        }

        public boolean isPassed() {
            // all the tests has been completed and passed
            return (isDeviceNewEnough == STATUS_PASSED)
                    && (recognitionCoreSupported == STATUS_PASSED)
                    && (hasCamera == STATUS_PASSED)
                    && (hasCameraPermission == STATUS_PASSED)
                    && (isCameraSupported == STATUS_PASSED);
        }

        public boolean isAdditionalCheckRequired() {
            return !isFailed() && !isPassed();
        }

        public boolean isFailedOnCameraPermission() {
            return (hasCameraPermission == STATUS_FAILED)
                    && (isDeviceNewEnough != STATUS_FAILED)
                    && (recognitionCoreSupported != STATUS_FAILED)
                    && (hasCamera != STATUS_FAILED)
                    && (isCameraSupported != STATUS_FAILED);
        }

        private String statusToString(int status) {
            switch (status) {
                case STATUS_PASSED: return "yes";
                case STATUS_FAILED: return "no";
                case STATUS_NOT_CHECKED: return "not checked";
                default: throw new IllegalArgumentException();
            }
        }

        public String getMessage() {
            if (isDeviceNewEnough == STATUS_FAILED) return "Device is considered being too old for smooth camera experience, so camera will not be used.";
            if (hasCamera == STATUS_FAILED) return "No camera";
            if (hasCameraPermission == STATUS_FAILED) return "No camera permission";
            if (isCameraSupported == STATUS_FAILED) return "Camera not supported";
            if (recognitionCoreSupported == STATUS_FAILED) return "Unsupported architecture";
            return toString();
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "Is new enough: %s, has camera: %s, has camera persmission: %s, recognition library supported: %s, camera supported: %s",
                    statusToString(isDeviceNewEnough),
                    statusToString(hasCamera),
                    statusToString(hasCameraPermission),
                    statusToString(recognitionCoreSupported),
                    statusToString(isCameraSupported)
                    );
        }
    }

    private static class RecognitionCheckResultBuilder {
        private boolean isBlockingCheck = true;
        private int hasCameraPermission = Result.STATUS_NOT_CHECKED;
        private int isDeviceNewEnough = Result.STATUS_NOT_CHECKED;
        private int recognitionCoreSupported = Result.STATUS_NOT_CHECKED;
        private int hasCamera = Result.STATUS_NOT_CHECKED;
        private int isCameraSupported = Result.STATUS_NOT_CHECKED;

        public RecognitionCheckResultBuilder() {
        }

        public RecognitionCheckResultBuilder isBlockingCheck(boolean isBlockingCheck) {
            this.isBlockingCheck = isBlockingCheck;
            return this;
        }

        public RecognitionCheckResultBuilder isDeviceNewEnough(boolean isDeviceNewEnough) {
            this.isDeviceNewEnough = toStatus(isDeviceNewEnough);
            return this;
        }

        public RecognitionCheckResultBuilder recognitionCoreSupported(boolean recognitionCoreSupported) {
            this.recognitionCoreSupported = toStatus(recognitionCoreSupported);
            return this;
        }

        public RecognitionCheckResultBuilder hasCamera(boolean hasCamera) {
            this.hasCamera = toStatus(hasCamera);
            return this;
        }

        public RecognitionCheckResultBuilder hasCameraPermission(boolean hasCameraPermission) {
            this.hasCameraPermission = toStatus(hasCameraPermission);
            return this;
        }

        public RecognitionCheckResultBuilder isCameraSupported(boolean isCameraSupported) {
            this.isCameraSupported = toStatus(isCameraSupported);
            return this;
        }


        public Result build() {
            return new Result(isBlockingCheck, isDeviceNewEnough, recognitionCoreSupported, hasCamera, hasCameraPermission, isCameraSupported);
        }

        private int toStatus(boolean isPassed) {
            return isPassed ? Result.STATUS_PASSED : Result.STATUS_FAILED;
        }
    }
}

