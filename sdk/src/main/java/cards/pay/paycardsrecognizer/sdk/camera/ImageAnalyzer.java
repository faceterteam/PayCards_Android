package cards.pay.paycardsrecognizer.sdk.camera;

import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_DATE;
import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_GRAB_CARD_IMAGE;
import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_NAME;
import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_NUMBER;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;
import java.util.Locale;

import cards.pay.paycardsrecognizer.sdk.camera.widget.CameraPreviewLayout;
import cards.pay.paycardsrecognizer.sdk.camera.widget.CardDetectionStateView;
import cards.pay.paycardsrecognizer.sdk.ndk.DisplayConfigurationImpl;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionCore;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionResult;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionStatusListener;
import cards.pay.paycardsrecognizer.sdk.utils.Constants;

public class ImageAnalyzer implements ImageAnalysis.Analyzer {

    public interface Callbacks {
        void onRecognitionComplete(RecognitionResult result);

        void onCardImageReceived(Bitmap bitmap);
    }

    private static final int DEFAULT_RECOGNITION_MODE = RECOGNIZER_MODE_NUMBER | RECOGNIZER_MODE_DATE
            | RECOGNIZER_MODE_NAME | RECOGNIZER_MODE_GRAB_CARD_IMAGE;

    private static final boolean DBG = Constants.DEBUG;

    private static final String TAG = "ImageAnalyzer";

    private final @RecognitionConstants.RecognitionMode int mRecognitionMode;
    private final Context mAppContext;

    private final Callbacks mCallbacks;
    private final RecognitionCore mRecognitionCore;
    private final CameraPreviewLayout mPreviewLayout;
    private final DisplayConfigurationImpl mDisplayConfiguration;

    public ImageAnalyzer(int recognitionMode, Context context, CameraPreviewLayout previewLayout, Callbacks callbacks) throws RuntimeException {

        if (recognitionMode == 0) recognitionMode = DEFAULT_RECOGNITION_MODE;
        mRecognitionMode = recognitionMode;
        mAppContext = context.getApplicationContext();
        mCallbacks = callbacks;
        mPreviewLayout = previewLayout;
        mRecognitionCore = RecognitionCore.getInstance(mAppContext);

        mDisplayConfiguration = new DisplayConfigurationImpl();
        mDisplayConfiguration.setDisplayParameters(getDisplay());
        mRecognitionCore.setDisplayConfiguration(mDisplayConfiguration);
    }

    @Override
    @ExperimentalGetImage
    public void analyze(@NonNull ImageProxy imageProxy) {

        if ((imageProxy.getFormat() == ImageFormat.YUV_420_888 ) && imageProxy.getPlanes().length == 3) {
            var yv12Data = convertYUV420888toYV12(imageProxy);

            int newBorders = mRecognitionCore.processFrameYV12(CameraUtils.CAMERA_RESOLUTION.size.width, CameraUtils.CAMERA_RESOLUTION.size.height, yv12Data);
            mPreviewLayout.getDetectionStateOverlay().setDetectionState(newBorders);
        }

        imageProxy.close();
    }

    // Constants for YV12 plane order
    public byte[] convertYUV420888toYV12(ImageProxy image) {
        var planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] yData = new byte[ySize];
        byte[] uData = new byte[uSize];
        byte[] vData = new byte[vSize];

        yBuffer.get(yData);
        uBuffer.get(uData);
        vBuffer.get(vData);

        byte[] yv12Data = new byte[ySize + uSize + vSize];

        // Copy Y plane
        System.arraycopy(yData, 0, yv12Data, 0, ySize);

        // Copy V plane
        System.arraycopy(vData, 0, yv12Data, ySize, vSize);

        // Copy U plane
        System.arraycopy(uData, 0, yv12Data, ySize + vSize, uSize);

        return yv12Data;
    }

    public void onResume() {
        if (DBG) Log.d(TAG, "onResume()");

        final Display display = getDisplay();
        mDisplayConfiguration.setDisplayParameters(display);
        mRecognitionCore.setDisplayConfiguration(mDisplayConfiguration);

        mRecognitionCore.setRecognitionMode(mRecognitionMode);
        mRecognitionCore.setStatusListener(mRecognitionStatusListener);
        mRecognitionCore.resetResult();

        mPreviewLayout.setOnWindowFocusChangedListener((view, hasWindowFocus) -> setRecognitionCoreIdle(!hasWindowFocus));

        getCardDetectionStateView().setRecognitionResult(RecognitionResult.empty());
        setRecognitionCoreIdle(false);
    }

    public void onPause() {
        if (DBG) Log.d(TAG, "onPause()");
        setRecognitionCoreIdle(true);
        mPreviewLayout.setOnWindowFocusChangedListener(null);
        mRecognitionCore.setStatusListener(null);
    }

    public void setSensorRotation(int rotation)
    {
        mDisplayConfiguration.setCameraParameters(rotation);
    }

    public void setRecognitionCoreIdle(boolean idle) {
        if (DBG) Log.d(TAG, "setRecognitionCoreIdle() called with: " + "idle = [" + idle + "]");
        mRecognitionCore.setIdle(idle);
    }

    public void setupCardDetectionCameraParameters(int previewSizeWidth, int previewSizeHeight, int sensorRotation) {
        /* Card on 720x1280 preview frame */
        Rect cardNdkRect = mRecognitionCore.getCardFrameRect();

        /* Card on 1280x720 preview frame */
        @SuppressWarnings("SuspiciousNameCombination")
        Rect cardCameraRect = OrientationHelper.rotateRect(cardNdkRect,
                CameraUtils.CAMERA_RESOLUTION.size.height,
                CameraUtils.CAMERA_RESOLUTION.size.width,
                90, null);

        mPreviewLayout.setCameraParameters(previewSizeWidth, previewSizeHeight,
                OrientationHelper.getCameraRotationToNatural(OrientationHelper.getDisplayRotationDegrees(getDisplay()), sensorRotation, false),
                cardCameraRect);
    }

    private CardDetectionStateView getCardDetectionStateView() {
        return mPreviewLayout.getDetectionStateOverlay();
    }

    private Display getDisplay() {
        return ((WindowManager) mAppContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    }

    private final RecognitionStatusListener mRecognitionStatusListener = new RecognitionStatusListener() {
        private long mRecognitionCompleteTs;

        @Override
        public void onRecognitionComplete(RecognitionResult result) {
            getCardDetectionStateView().setRecognitionResult(result);
            if (result.isFirst()) {
                getCardDetectionStateView().setDetectionState(RecognitionConstants.DETECTED_BORDER_TOP
                        | RecognitionConstants.DETECTED_BORDER_LEFT
                        | RecognitionConstants.DETECTED_BORDER_RIGHT
                        | RecognitionConstants.DETECTED_BORDER_BOTTOM
                );
                if (DBG) mRecognitionCompleteTs = System.nanoTime();
            }
            if (result.isFinal()) {
                long newTs = System.nanoTime();
                if (DBG)
                    Log.v(TAG, String.format(Locale.US, "Final result received after %.3f ms", (newTs - mRecognitionCompleteTs) / 1_000_000f));
            }
            mCallbacks.onRecognitionComplete(result);
        }

        @Override
        public void onCardImageReceived(Bitmap bitmap) {
            if (DBG) {
                long newTs = System.nanoTime();
                Log.v(TAG, String.format(Locale.US, "Card image received after %.3f ms", (newTs - mRecognitionCompleteTs) / 1_000_000f));
            }
            mCallbacks.onCardImageReceived(bitmap);
        }
    };
}
