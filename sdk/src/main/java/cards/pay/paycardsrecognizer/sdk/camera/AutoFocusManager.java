package cards.pay.paycardsrecognizer.sdk.camera;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import cards.pay.paycardsrecognizer.sdk.utils.Constants;

import static android.hardware.Camera.AutoFocusCallback;
import static android.hardware.Camera.AutoFocusMoveCallback;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class AutoFocusManager {

    private static final boolean DBG = Constants.DEBUG;
    private static final String TAG = "AutoFocusManager";

    private final Camera mCamera;

    @Nullable
    private final FocusMoveCallback mCallback;

    @Nullable
    private FocusManager mFocusManager;

    private Handler mHandler;

    public interface FocusMoveCallback {
        void onAutoFocusMoving(boolean start, Camera camera);
        void onAutoFocusComplete(boolean success, Camera camera);
    }

    public AutoFocusManager(Camera camera, @Nullable FocusMoveCallback callback) {
        mCamera = camera;
        mCallback = callback;
        mHandler = new Handler(Looper.myLooper());
    }

    public void start() {
        if (mFocusManager != null) {
            mFocusManager.stop();
            mFocusManager = null;
        }
        if (isCameraFocusContinuous()) {
            mFocusManager = new AutoFocusManagerImpl(mCamera, mCallback, mHandler);
            mFocusManager.start();
            if (DBG) Log.d(TAG, "start(): camera continuous focus");
        } else if (isCameraFocusManual()) {
            mFocusManager = new ManualFocusManagerImpl(mCamera, mCallback, mHandler);
            mFocusManager.start();
            if (DBG) Log.d(TAG, "start(): focus with manual reset");
        } else {
            // Focus is fixed. Ignore
        }
    }

    public void stop() {
        if (mFocusManager != null) {
            mFocusManager.stop();
            mFocusManager = null;
        }
    }

    public boolean isStarted() {
        return mFocusManager !=  null;
    }

    public void requestFocus() {
        if (mFocusManager != null) {
            mFocusManager.requestFocus();
        }
    }

    private boolean isCameraFocusContinuous() {
        String focusMode = mCamera.getParameters().getFocusMode();
        return Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(focusMode)
                || Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO.equals(focusMode)
                || Camera.Parameters.FOCUS_MODE_EDOF .equals(focusMode);
    }

    private boolean isCameraFocusFixed() {
        String focusMode = mCamera.getParameters().getFocusMode();
        return Camera.Parameters.FOCUS_MODE_INFINITY.equals(focusMode)
                || Camera.Parameters. FOCUS_MODE_FIXED.equals(focusMode);
    }

    private boolean isCameraFocusManual() {
        String focusMode = mCamera.getParameters().getFocusMode();
        return Camera.Parameters.FOCUS_MODE_AUTO.equals(focusMode)
                || Camera.Parameters. FOCUS_MODE_MACRO.equals(focusMode);
    }

    private interface FocusManager {
        void start();
        void stop();
        void requestFocus();
    }


    private static class ManualFocusManagerImpl implements FocusManager {

        private static final int FOCUS_DELAY_FAST = 500;

        private static final int FOCUS_DELAY_SLOW = 3000;

        private final Camera mCamera;

        @Nullable
        private final FocusMoveCallback mCallback;

        private final Handler mHandler;

        private boolean mIsFocusMoving;

        private static boolean sFocusCompleteWorking;

        @TargetApi(16)
        public ManualFocusManagerImpl(Camera camera, @Nullable FocusMoveCallback callback, Handler handler) {
            this.mCamera = camera;
            this.mCallback = callback;
            this.mHandler = handler;
            if (Build.VERSION.SDK_INT >= 16 && this.mCallback != null) {
                mCamera.setAutoFocusMoveCallback(new AutoFocusMoveCallback() {
                    @Override
                    public void onAutoFocusMoving(boolean start, Camera camera) {
                        mCallback.onAutoFocusMoving(start, camera);
                    }
                });
            }
        }

        @Override
        public void start() {
            cancelAutoFocusSafe();
            restartCounter(FOCUS_DELAY_FAST);
        }

        @Override
        public void stop() {
            mHandler.removeCallbacks(mRequestFocusRunnable);
            cancelAutoFocusSafe();
        }

        @Override
        public void requestFocus() {
            if (!mIsFocusMoving || !sFocusCompleteWorking) {
                cancelAutoFocusSafe();
                restartCounter(0);
            }
        }

        private void restartCounter(int delay) {
            mHandler.removeCallbacks(mRequestFocusRunnable);
            if (delay == 0) {
                mHandler.post(mRequestFocusRunnable);
            } else {
                mHandler.postDelayed(mRequestFocusRunnable, delay);
            }
        }

        private final Runnable mRequestFocusRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    mCamera.autoFocus(mAutoFocusCallback);
                    mIsFocusMoving = true;
                    if (mCallback != null) mCallback.onAutoFocusMoving(true, mCamera);
                } catch (final Exception ignored) {
                    mIsFocusMoving = false;
                    if (mCallback != null) mCallback.onAutoFocusMoving(false, mCamera);
                }
            }
        };

        private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback() {
            @Override
            public void onAutoFocus(final boolean success, final Camera camera) {
                if (mCallback != null) mCallback.onAutoFocusComplete(success, camera);

                mIsFocusMoving = false;

                if (!sFocusCompleteWorking) {
                    sFocusCompleteWorking = true;
                    if (DBG) Log.d(TAG, "onAutoFocus() onAutoFocus callback looks like working");
                }

                restartCounter(success ? FOCUS_DELAY_SLOW : FOCUS_DELAY_FAST);
            }
        };

        private void cancelAutoFocusSafe() {
            try {
                mCamera.cancelAutoFocus();
            } catch (RuntimeException e) {
                // IGNORE
            }
        }
    }


    /**
     * {@link Camera.Parameters#FOCUS_MODE_CONTINUOUS_PICTURE} and {@link Camera.Parameters#FOCUS_MODE_CONTINUOUS_VIDEO}
     * focus mode
     */
    private static class AutoFocusManagerImpl implements FocusManager {

        private static final int FOCUS_RESET_DELAY = 1000;

        private final Camera mCamera;

        @Nullable
        private final FocusMoveCallback mCallback;

        private final Handler mHandler;

        private boolean mCameraMoving;

        @TargetApi(16)
        public AutoFocusManagerImpl(Camera camera, @Nullable FocusMoveCallback callback, Handler handler) {
            this.mCamera = camera;
            this.mCallback = callback;
            this.mHandler = handler;
            if (Build.VERSION.SDK_INT >= 16 && this.mCallback != null) {
                mCamera.setAutoFocusMoveCallback(new AutoFocusMoveCallback() {
                    @Override
                    public void onAutoFocusMoving(boolean start, Camera camera) {
                        mCallback.onAutoFocusMoving(start, camera);
                        mCameraMoving = start;
                    }
                });
            }
        }

        @Override
        public void start() {
            resumeAutoFocus();
            restartCounter(FOCUS_RESET_DELAY);
        }

        @Override
        public void stop() {
            mHandler.removeCallbacks(mResetFocusRunnable);
        }

        @Override
        public void requestFocus() {
            if (!mCameraMoving) {
                cancelAutoFocusSafe();
                restartCounter(FOCUS_RESET_DELAY);
            } else {
                if (DBG) Log.d(TAG, "requestFocus(): ignore since camera is moving");
            }
        }

        private void resumeAutoFocus() {
            //if (DBG) Log.d(TAG, "resumeAutoFocus()");
            cancelAutoFocusSafe();
        }

        private void restartCounter(int delay) {
            mHandler.removeCallbacks(mResetFocusRunnable);
            if (delay == 0) {
                mHandler.post(mResetFocusRunnable);
            } else {
                mHandler.postDelayed(mResetFocusRunnable, delay);
            }
        }

        private final Runnable mResetFocusRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    resumeAutoFocus();
                    restartCounter(FOCUS_RESET_DELAY);
                } catch (final Exception ignored) {
                    // ignore
                }
            }
        };

        private void cancelAutoFocusSafe() {
            try {
                mCamera.cancelAutoFocus();
            } catch (RuntimeException e) {
                // IGNORE
            }
        }
    }

}
