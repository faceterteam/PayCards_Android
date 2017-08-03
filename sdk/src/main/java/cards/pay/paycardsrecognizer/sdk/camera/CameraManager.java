package cards.pay.paycardsrecognizer.sdk.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionCore;
import cards.pay.paycardsrecognizer.sdk.utils.Constants;

class CameraManager {

    private static final boolean DBG = Constants.DEBUG;
    private static final String TAG = "RenderNCamThreadCamera";

    private final Context mAppContext;

    private final RecognitionCore mRecognitionCore;

    @Nullable
    private Camera mCamera;

    private AutoFocusManager mAutoFocusManager;

    private TorchManager mTorchManager;

    private volatile ProcessFrameThread mProcessThread;

    private AutoFocusManager.FocusMoveCallback mFocusCallbacks;

    private ProcessFrameThread.Callbacks mProcessFrameCallbacks;

    private volatile Camera.PreviewCallback mSnapNextFrameCallback;

    private boolean mIsResumed;

    private boolean mIsProcessFramesActive;

    public CameraManager(Context context) {
        mAppContext = context.getApplicationContext();
        mRecognitionCore = RecognitionCore.getInstance(mAppContext);
        mIsResumed = true;
        mIsProcessFramesActive = true;
    }

    @Nullable
    public Camera getCamera() {
        return mCamera;
    }

    public synchronized boolean isOpen() {
        return mCamera != null;
    }

    public Camera.Size getCurrentPreviewSize() {
        if (mCamera == null) return null;
        Camera.Parameters parameters = mCamera.getParameters();
        return parameters.getPreviewSize();
    }

    public int getSensorOrientation() {
        return CameraUtils.getBackCameraSensorOrientation();
    }

    public int calculateDataRotation() {
        Display display = ((WindowManager)mAppContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        return CameraUtils.getBackCameraDataRotation(display);
    }

    public synchronized void openCamera() throws Exception {
        if (mCamera != null) releaseCamera();
        openCameraInternal();

        mAutoFocusManager = new AutoFocusManager(mCamera, mFocusCallbacks);
        syncAutofocusManager();

        mTorchManager = new TorchManager(mRecognitionCore, mCamera);
        syncTorchManager();

        syncProcessThread(true);
    }

    public synchronized void releaseCamera() {
        if (DBG) Log.d(TAG, "releaseCamera()");

        stopProcessThread();

        if (mAutoFocusManager != null) {
            mAutoFocusManager.stop();
            mAutoFocusManager = null;
        }
        if (mTorchManager != null) {
            mTorchManager.destroy();
            mTorchManager = null;
        }
        if (mCamera != null) {
            // release the camera for other applications
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void setAutoFocusCallbacks(AutoFocusManager.FocusMoveCallback callback) {
        mFocusCallbacks = callback;
    }

    public void setProcessFrameCallbacks(ProcessFrameThread.Callbacks callbacks) {
        mProcessFrameCallbacks = callbacks;
    }

    public void toggleFlash() {
        if (DBG) Log.d(TAG, "toggleFlash()");
        if (mTorchManager == null) return;
        mTorchManager.toggleTorch();
    }

    public void requestFocus() {
        if (DBG) Log.d(TAG, "requestFocus()");
        if (mAutoFocusManager != null) mAutoFocusManager.requestFocus();
    }

    public synchronized void pause() {
        if (DBG) Log.d(TAG, "pause()");
        if (!mIsResumed) return;
        mIsResumed = false;
        syncAutofocusManager();
        syncTorchManager();
        syncProcessThread(false);
    }

    public synchronized void resume() {
        if (DBG) Log.d(TAG, "resume(); is resumed already: " + mIsResumed);
        if (mIsResumed) return;
        mIsResumed = true;
        syncAutofocusManager();
        syncTorchManager();
        syncProcessThread(false);
    }

    public synchronized void resumeProcessFrames() {
        if (DBG) Log.d(TAG, "resumeProcessFrames(); frames processed already: " + mIsProcessFramesActive);
        if (mIsProcessFramesActive) return;
        mIsProcessFramesActive = true;
        syncAutofocusManager();
        syncTorchManager();
        syncProcessThread(false);
    }

    public synchronized void pauseProcessFrames() {
        if (!mIsProcessFramesActive) return;
        mIsProcessFramesActive = false;
        syncAutofocusManager();
        syncTorchManager();
        syncProcessThread(false);
    }

    void startPreview(SurfaceTexture texture) throws IOException, RuntimeException {
        if (DBG) Log.d(TAG, "startPreview() called with: " +  "texture = [" + texture + "]");
        try {
            if (mCamera != null) {
                mCamera.setPreviewTexture(texture);
                mCamera.startPreview();
            } else {
                if (DBG) Log.e(TAG, "Camera is not opened. Skip startPreview()");
            }
        } catch (IOException | RuntimeException e) {
            releaseCamera();
            throw e;
        }
    }


    private void openCameraInternal() throws Exception {
        if (mCamera != null) releaseCamera();

        try {
            mCamera = Camera.open();

            CameraUtils.NativeSupportedSize supportedSize;

            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            supportedSize = CameraUtils.findBestCameraSupportedSize(supportedPreviewSizes);
            if (supportedSize == CameraUtils.NativeSupportedSize.RESOLUTION_NO_CAMERA) {
                throw new RecognitionUnavailableException(RecognitionUnavailableException.ERROR_CAMERA_NOT_SUPPORTED);
            }

            parameters.setPreviewSize(supportedSize.size.width, supportedSize.size.height);
            parameters.setPreviewFormat(ImageFormat.YV12);
            CameraConfigurationUtils.setBestExposure(parameters, false);

            CameraConfigurationUtils.initWhiteBalance(parameters);
            CameraConfigurationUtils.initAutoFocus(parameters);

            // parameters.setRecordingHint(true);
            CameraConfigurationUtils.setMetering(parameters);

            mCamera.setParameters(parameters);

            // if (DBG) Log.v(TAG, "Camera parameters: " + mCamera.getParameters().flatten().replace(";", "; "));
        } catch (Exception e) {
            // Something bad happened
            if (DBG) Log.e(TAG, "startCamera() error: ", e);
            releaseCamera();
            throw e;
        }
    }

    private synchronized boolean isTorchManagerShouldBeActive() {
        return (mCamera != null) && mIsResumed && mIsProcessFramesActive;
    }

    private boolean isAutofocusShouldBeActive() {
        return (mCamera != null) && mIsResumed;
    }

    private synchronized void syncTorchManager() {
        if (mTorchManager != null) {
            if (isTorchManagerShouldBeActive()) {
                mTorchManager.resume();
            } else {
                mTorchManager.pause();
            }
        }
    }

    private synchronized void syncAutofocusManager() {
        if (mAutoFocusManager != null) {
            if (isAutofocusShouldBeActive()) {
                mAutoFocusManager.start();
            } else {
                mAutoFocusManager.stop();
            }
        }
    }

    private synchronized void syncProcessThread(boolean forceRestart) {
        if (mIsResumed && mIsProcessFramesActive && (mCamera != null)) {
            if (forceRestart || (mProcessThread == null)) startProcessThread();
        } else {
            if (mProcessThread != null) stopProcessThread();
        }
    }

    private synchronized void startProcessThread() {
        if (mCamera == null) {
            if (DBG) Log.e(TAG, "Camera is not opened. Skip startProcessThread()");
            return;
        }
        stopProcessThread();
        mProcessThread = new ProcessFrameThread(mAppContext, mCamera, new ProcessFrameThread.Callbacks() {
            @Override
            public void onFrameProcessed(int newBorders) {
                if (mProcessFrameCallbacks != null) mProcessFrameCallbacks.onFrameProcessed(newBorders);

            }

            @Override
            public void onFpsReport(String report) {
                //if (DBG) Log.v(TAG, report);
                if (mProcessFrameCallbacks != null) mProcessFrameCallbacks.onFpsReport(report);

            }
        });
        mProcessThread.start();

        final ProcessFrameThread thread = mProcessThread;
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            Camera.PreviewCallback singleFrameCallback;
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (mCamera == null) return;
                if (DBG) {
                    singleFrameCallback = mSnapNextFrameCallback;
                    mSnapNextFrameCallback = null;
                    if (singleFrameCallback != null) singleFrameCallback.onPreviewFrame(data, camera);
                }
                thread.processFrame(data);
            }
        });
        Camera.Size size = mCamera.getParameters().getPreviewSize();
        for (int i = 0; i < 3; ++i) {
            mCamera.addCallbackBuffer(new byte[size.width * size.height * 3 / 2]);
        }
    }

    private synchronized void stopProcessThread() {
        if (DBG) Log.d(TAG, "stopProcessThread()");
        if (mProcessThread != null) {
            mProcessThread.setActive(false);
            mProcessThread = null;
            if (mCamera != null) mCamera.setPreviewCallbackWithBuffer(null);
        }
    }
}
