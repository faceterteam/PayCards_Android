package cards.pay.paycardsrecognizer.sdk.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import java.util.Locale;

import cards.pay.paycardsrecognizer.sdk.camera.widget.CameraPreviewLayout;
import cards.pay.paycardsrecognizer.sdk.camera.widget.CardDetectionStateView;
import cards.pay.paycardsrecognizer.sdk.camera.widget.OnWindowFocusChangedListener;
import cards.pay.paycardsrecognizer.sdk.ndk.DisplayConfigurationImpl;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionCore;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionResult;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionStatusListener;
import cards.pay.paycardsrecognizer.sdk.utils.Constants;

import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_DATE;
import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_GRAB_CARD_IMAGE;
import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_NAME;
import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_NUMBER;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ScanManager {

    private static final int DEFAULT_RECOGNITION_MODE = RECOGNIZER_MODE_NUMBER | RECOGNIZER_MODE_DATE
            | RECOGNIZER_MODE_NAME | RECOGNIZER_MODE_GRAB_CARD_IMAGE;

    private static final boolean DBG = Constants.DEBUG;
    private static final String TAG = "ScanManager";

    private static SurfaceHolder sSurfaceHolder;

    private final @RecognitionConstants.RecognitionMode int mRecognitionMode;

    private final Context mAppContext;

    private final Callbacks mCallbacks;

    private RecognitionCore mRecognitionCore;

    private CameraPreviewLayout mPreviewLayout;

    // Receives messages from renderer thread.
    private ScanManagerHandler mHandler;

    // Thread that handles rendering and controls the camera.  Started in onResume(),
    // stopped in onPause().
    @Nullable
    private RenderThread mRenderThread;

    private final WindowRotationListener mWindowRotationListener;

    private final DisplayConfigurationImpl mDisplayConfiguration;

    public interface Callbacks {
        void onCameraOpened(Camera.Parameters cameraParameters);
        void onOpenCameraError(Exception exception);
        void onRecognitionComplete(RecognitionResult result);
        void onCardImageReceived(Bitmap bitmap);
        void onFpsReport(String report);
        void onAutoFocusMoving(boolean start, String cameraFocusMode);
        void onAutoFocusComplete(boolean success, String cameraFocusMode);
    }

    public ScanManager(Context context, CameraPreviewLayout previewLayout, Callbacks callbacks) {
        this(DEFAULT_RECOGNITION_MODE, context, previewLayout, callbacks);
    }

    public ScanManager(int recognitionMode, Context context, CameraPreviewLayout previewLayout, Callbacks callbacks) throws RuntimeException {
        if (recognitionMode == 0) recognitionMode = DEFAULT_RECOGNITION_MODE;
        mRecognitionMode = recognitionMode;
        mAppContext = context.getApplicationContext();
        mCallbacks = callbacks;
        mPreviewLayout = previewLayout;
        mRecognitionCore = RecognitionCore.getInstance(mAppContext);
        mHandler = new ScanManagerHandler(this);

        Display display = getDisplay();
        mDisplayConfiguration = new DisplayConfigurationImpl();
        mDisplayConfiguration.setCameraParameters(CameraUtils.getBackCameraSensorOrientation());
        mDisplayConfiguration.setDisplayParameters(display);
        mRecognitionCore.setDisplayConfiguration(mDisplayConfiguration);

        SurfaceHolder sh = getSurfaceView().getHolder();
        sh.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (DBG) Log.d(TAG, "SurfaceView  surfaceCreated holder=" + holder + " (static=" + sSurfaceHolder + ")");
                if (sSurfaceHolder != null) {
                    throw new RuntimeException("sSurfaceHolder is already set");
                }

                sSurfaceHolder = holder;

                if (mRenderThread != null) {
                    // Normal case -- render thread is running, tell it about the new surface.
                    RenderThread.RenderHandler rh = mRenderThread.getHandler();
                    rh.sendSurfaceAvailable(holder, true);
                } else {
                    // Sometimes see this on 4.4.x N5: power off, power on, unlock, with device in
                    // landscape and a lock screen that requires portrait.  The surface-created
                    // message is showing up after onPause().
                    //
                    // Chances are good that the surface will be destroyed before the activity is
                    // unpaused, but we track it anyway.  If the activity is un-paused and we start
                    // the RenderThread, the SurfaceHolder will be passed in right after the thread
                    // is created.
                    if (DBG) Log.d(TAG, "render thread not running");
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (DBG) Log.d(TAG, "SurfaceView surfaceChanged fmt=" + format + " size=" + width + "x" + height +
                        " holder=" + holder);

                if (mRenderThread != null) {
                    RenderThread.RenderHandler rh = mRenderThread.getHandler();
                    rh.sendSurfaceChanged(format, width, height);
                } else {
                    if (DBG) Log.d(TAG, "Ignoring surfaceChanged");
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // In theory we should tell the RenderThread that the surface has been destroyed.
                if (mRenderThread != null) {
                    RenderThread.RenderHandler rh = mRenderThread.getHandler();
                    rh.sendSurfaceDestroyed();
                }
                if (DBG) Log.d(TAG, "SurfaceView surfaceDestroyed holder=" + holder);
                sSurfaceHolder = null;
            }
        });

        mWindowRotationListener = new WindowRotationListener();
    }

    public void onResume() {
        if (DBG) Log.d(TAG, "onResume()");

        mRenderThread = new RenderThread(mAppContext, mHandler);
        mRenderThread.setName("Camera thread");
        mRenderThread.start();
        mRenderThread.waitUntilReady();

        RenderThread.RenderHandler rh = mRenderThread.getHandler();
        if (sSurfaceHolder != null) {
            if (DBG) Log.d(TAG, "Sending previous surface");
            rh.sendSurfaceAvailable(sSurfaceHolder, false);
        } else {
            if (DBG) Log.d(TAG, "No previous surface");
        }

        mDisplayConfiguration.setCameraParameters(CameraUtils.getBackCameraSensorOrientation());
        mRecognitionCore.setRecognitionMode(mRecognitionMode);
        mRecognitionCore.setStatusListener(mRecognitionStatusListener);
        mRecognitionCore.resetResult();

        RenderThread.RenderHandler handler = mRenderThread.getHandler();
        handler.sendOrientationChanged(CameraUtils.getBackCameraDataRotation(getDisplay()));
        handler.sendUnfreeze();

        mPreviewLayout.setOnWindowFocusChangedListener(new OnWindowFocusChangedListener() {
            @Override
            public void onWindowFocusChanged(View view, boolean hasWindowFocus) {
                if (hasWindowFocus) {
                    setRecognitionCoreIdle(false);
                } else {
                    setRecognitionCoreIdle(true);
                }
            }
        });

        startShakeDetector();
        mWindowRotationListener.register(mAppContext, getDisplay(), new WindowRotationListener.RotationListener() {
            @Override
            public void onWindowRotationChanged() {
                refreshDisplayOrientation();
            }
        });
        getCardDetectionStateView().setRecognitionResult(RecognitionResult.empty());
        setRecognitionCoreIdle(false);
    }

    public void onPause() {
        if (DBG) Log.d(TAG, "onPause()");
        setRecognitionCoreIdle(true);
        stopShakeDetector();
        mPreviewLayout.setOnWindowFocusChangedListener(null);
        mRecognitionCore.setStatusListener(null);

        if (mRenderThread != null) {
            RenderThread.RenderHandler rh = mRenderThread.getHandler();
            rh.sendShutdown();
            try {
                mRenderThread.join();
            } catch (InterruptedException ie) {
                // not expected
                if (mCallbacks != null) mCallbacks.onOpenCameraError(ie);
            }
            mRenderThread = null;
        }
        mWindowRotationListener.unregister();
    }

    public void resumeScan() {
        setRecognitionCoreIdle(false);
    }

    public void toggleFlash() {
        if (mRenderThread == null) return;
        RenderThread.RenderHandler rh = mRenderThread.getHandler();
        rh.sendToggleFlash();
    }

    private SurfaceView getSurfaceView() {
        return mPreviewLayout.getSurfaceView();
    }

    private CardDetectionStateView getCardDetectionStateView() {
        return mPreviewLayout.getDetectionStateOverlay();
    }

    private Display getDisplay() {
        return ((WindowManager)mAppContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    }

    public void resetResult() {
        if (DBG) Log.d(TAG, "resetResult()");
        mRecognitionCore.resetResult();
        if (mRenderThread != null) {
            RenderThread.RenderHandler rh = mRenderThread.getHandler();
            rh.sendResumeProcessFrames();
        }

        unfreezeCameraPreview();
    }

    private void refreshDisplayOrientation() {
        if (DBG) Log.d(TAG, "refreshDisplayOrientation()");
        final Display display = getDisplay();
        mDisplayConfiguration.setDisplayParameters(display);
        mRecognitionCore.setDisplayConfiguration(mDisplayConfiguration);
        if (mRenderThread != null) {
            int rotation = CameraUtils.getBackCameraDataRotation(display);
            mRenderThread.getHandler().sendOrientationChanged(rotation);
        }
    }

    public void setRecognitionCoreIdle(boolean idle) {
        if (DBG) Log.d(TAG, "setRecognitionCoreIdle() called with: " +  "idle = [" + idle + "]");
        mRecognitionCore.setIdle(idle);
        if (mRenderThread != null) {
            if (idle) {
                mRenderThread.getHandler().sendPauseCamera();
            } else {
                mRenderThread.getHandler().sendResumeCamera();
            }
        }
    }

    private void setupCardDetectionCameraParameters(int previewSizeWidth, int previewSizeHeight) {
        /* Card on 720x1280 preview frame */
        Rect cardNdkRect = mRecognitionCore.getCardFrameRect();

        /* Card on 1280x720 preview frame */
        @SuppressWarnings("SuspiciousNameCombination")
        Rect cardCameraRect = OrientationHelper.rotateRect(cardNdkRect,
                CameraUtils.CAMERA_RESOLUTION.size.height,
                CameraUtils.CAMERA_RESOLUTION.size.width,
                90, null);

        mPreviewLayout.setCameraParameters(previewSizeWidth, previewSizeHeight,
                CameraUtils.getBackCameraDataRotation(getDisplay()),
                cardCameraRect);
    }

    @MainThread
    void onCameraOpened(Camera.Parameters parameters) {
        Camera.Size previewSize = parameters.getPreviewSize();
        setupCardDetectionCameraParameters(previewSize.width, previewSize.height);
        if (mCallbacks != null) mCallbacks.onCameraOpened(parameters);
    }

    @MainThread
    void onOpenCameraError(Exception e) {
        if (DBG) Log.d(TAG, "onOpenCameraError() called with: " +  "e = [" + e + "]");
        if (mCallbacks != null) mCallbacks.onOpenCameraError(e);
        mRenderThread = null;
    }

    @MainThread
    void onRenderThreadError(Throwable e) {
        // XXX
        if (DBG) Log.d(TAG, "onRenderThreadError() called with: " +  "e = [" + e + "]");
        if (mCallbacks != null) mCallbacks.onOpenCameraError((Exception) e);
        mRenderThread = null;
    }

    @MainThread
    void onFrameProcessed(int newBorders) {
        if (mCallbacks != null) mPreviewLayout.getDetectionStateOverlay().setDetectionState(newBorders);
    }

    @MainThread
    void onFpsReport(String fpsReport) {
        if (mCallbacks != null) mCallbacks.onFpsReport(fpsReport);
    }

    @MainThread
    void onAutoFocusMoving(boolean isStart, String focusMode) {
        if (mCallbacks != null) mCallbacks.onAutoFocusMoving(isStart, focusMode);
    }

    @MainThread
    void onAutoFocusComplete(boolean isSuccess, String focusMode) {
        if (mCallbacks != null) mCallbacks.onAutoFocusComplete(isSuccess, focusMode);
    }

    public void freezeCameraPreview() {
        if (DBG) Log.d(TAG, "freezeCameraPreview() called with: " +  "");
        if (mRenderThread != null) mRenderThread.getHandler().sendFreeze();
    }

    public void unfreezeCameraPreview() {
        if (DBG) Log.d(TAG, "unfreezeCameraPreview() called with: " +  "");
        if (mRenderThread != null) {
            mRenderThread.getHandler().sendUnfreeze();
        }
    }

    private void startShakeDetector() {
        SensorManager sensorManager = (SensorManager) mAppContext.getSystemService(Context.SENSOR_SERVICE);
        final Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (null != sensor) {
            sensorManager.registerListener(mShakeEventListener, sensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    private void stopShakeDetector() {
        SensorManager sensorManager = (SensorManager) mAppContext.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(mShakeEventListener);
    }

    private final RecognitionStatusListener mRecognitionStatusListener = new RecognitionStatusListener() {
        private long mRecognitionCompleteTs;

        @Override
        public void onRecognitionComplete(RecognitionResult result) {
            getCardDetectionStateView().setRecognitionResult(result);
            if (result.isFirst()) {
                if (mRenderThread != null) mRenderThread.getHandler().sendPauseProcessFrames();
                getCardDetectionStateView().setDetectionState(RecognitionConstants.DETECTED_BORDER_TOP
                        | RecognitionConstants.DETECTED_BORDER_LEFT
                        | RecognitionConstants.DETECTED_BORDER_RIGHT
                        | RecognitionConstants.DETECTED_BORDER_BOTTOM
                );
                if (DBG) mRecognitionCompleteTs = System.nanoTime();
            }
            if (result.isFinal()) {
                long newTs = System.nanoTime();
                if (DBG) Log.v(TAG, String.format(Locale.US, "Final result received after %.3f ms", (newTs - mRecognitionCompleteTs) / 1_000_000f));
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

    private final SensorEventListener mShakeEventListener = new SensorEventListener() {

        private static final double SHAKE_THRESHOLD = 3.3;

        long lastUpdate;
        public double[] gravity = new double[3];

        @Override
        public void onSensorChanged(final SensorEvent event) {
            final long curTime = System.currentTimeMillis();
            // only allow one update every 100ms.
            final long diffTime = (curTime - lastUpdate);
            if (500 < diffTime) {
                lastUpdate = curTime;

                final float alpha = 0.8f;
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                final double x = event.values[0] - gravity[0];
                final double y = event.values[1] - gravity[1];
                final double z = event.values[2] - gravity[2];

                final double speed = Math.sqrt(x * x + y * y + z * z);

                if (SHAKE_THRESHOLD < speed) {
                    if (mRenderThread != null) {
                        if (DBG) Log.d(TAG, "shake focus request");
                        mRenderThread.getHandler().sendRequestFocus();
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(final Sensor sensor, final int accuracy) {

        }
    };
}
