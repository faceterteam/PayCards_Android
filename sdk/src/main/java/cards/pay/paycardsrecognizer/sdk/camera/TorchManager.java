package cards.pay.paycardsrecognizer.sdk.camera;

import android.hardware.Camera;
import android.support.annotation.RestrictTo;
import android.util.Log;

import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionCore;
import cards.pay.paycardsrecognizer.sdk.ndk.TorchStatusListener;
import cards.pay.paycardsrecognizer.sdk.utils.Constants;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class TorchManager {

    private static final boolean DBG = Constants.DEBUG;
    private static final String TAG = "TorchManager";

    private final Camera mCamera;

    private boolean mPaused;

    private boolean mTorchTurnedOn;

    private final RecognitionCore mRecognitionCore;

    public TorchManager(RecognitionCore recognitionCore, Camera camera) {
        mCamera = camera;
        mRecognitionCore = recognitionCore;
    }

    public void pause() {
        if (DBG) Log.d(TAG, "pause()");
        CameraConfigurationUtils.setFlashLight(mCamera, false);
        mPaused = true;
        mRecognitionCore.setTorchListener(null);
    }

    public void resume() {
        if (DBG) Log.d(TAG, "resume()");
        mPaused = false;
        mRecognitionCore.setTorchListener(mRecognitionCoreTorchStatusListener);
        if (mTorchTurnedOn) {
            mRecognitionCore.setTorchStatus(true);
        } else {
            mRecognitionCore.setTorchStatus(false);
        }
    }

    public void destroy() {
        mRecognitionCore.setTorchListener(null);
    }

    private boolean isTorchTurnedOn() {
        String flashMode = mCamera.getParameters().getFlashMode();
        return Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)
                || Camera.Parameters.FLASH_MODE_ON.equals(flashMode);
    }

    public void toggleTorch() {
        if (mPaused) return;
        boolean newStatus = !isTorchTurnedOn();
        if (DBG) Log.d(TAG, "toggleTorch() called with newStatus: " +  newStatus);
        mRecognitionCore.setTorchStatus(newStatus);

        // onTorchStatusChanged() will not be called if the RecognitionCore internal status will not be changed.
        // Sync twice to keep safe
        CameraConfigurationUtils.setFlashLight(mCamera, newStatus);
    }

    private final TorchStatusListener mRecognitionCoreTorchStatusListener = new TorchStatusListener() {

        // called from RecognitionCore
        @Override
        public void onTorchStatusChanged(boolean turnTorchOn) {
            if (mCamera == null) return;
            if (DBG) Log.d(TAG, "onTorchStatusChanged() called with: " +  "turnTorchOn = [" + turnTorchOn + "]");
            if (turnTorchOn) {
                mTorchTurnedOn = true;
                if (!mPaused) CameraConfigurationUtils.setFlashLight(mCamera, true);
            } else {
                mTorchTurnedOn = false;
                CameraConfigurationUtils.setFlashLight(mCamera, false);
            }
        }
    };
}
