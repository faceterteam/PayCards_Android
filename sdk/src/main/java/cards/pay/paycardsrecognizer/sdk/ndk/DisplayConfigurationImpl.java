package cards.pay.paycardsrecognizer.sdk.ndk;

import android.annotation.SuppressLint;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.Display;

import cards.pay.paycardsrecognizer.sdk.BuildConfig;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.WorkAreaOrientation;

import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.WORK_AREA_ORIENTATION_LANDSCAPE_LEFT;
import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.WORK_AREA_ORIENTATION_LANDSCAPE_RIGHT;
import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.WORK_AREA_ORIENTATION_PORTAIT;
import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.WORK_AREA_ORIENTATION_PORTAIT_UPSIDE_DOWN;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DisplayConfigurationImpl implements DisplayConfiguration {

    private static final String TAG = "DisplayConfigImpl";

    private static final boolean DBG = BuildConfig.DEBUG;

    private static final int LANDSCAPE_ORIENTATION_CORRECTION = -90;

    // rotation of the screen from its "natural" orientation.
    private int mDisplayRotation;

    private boolean mNaturalOrientationIsLandscape;

    // The orientation of the camera image. The value is the angle that the camera image needs
    // to be rotated clockwise so it shows correctly on the display in its natural orientation.
    private int mCameraSensorRotation  = 0;

    private int mPreprocessFrameRotation = 0;

    public DisplayConfigurationImpl() {
    }

    public void setDisplayParameters(Display display) {
        setDisplayParameters(DisplayHelper.getDisplayRotationDegrees(display),
                DisplayHelper.naturalOrientationIsLandscape(display)
                );
    }

    @VisibleForTesting
    void setDisplayParameters(int displayRotation, boolean naturalOrientationIsLandscape) {
        mDisplayRotation = displayRotation;
        mNaturalOrientationIsLandscape = naturalOrientationIsLandscape;
        if (DBG) Log.d(TAG, "setDisplayParameters() called with: "
                +  "rotation: "+ mDisplayRotation
                +  "; natural orientation: " + (mNaturalOrientationIsLandscape ? "landscape" : "portait (or square)"));
        refreshPreprocessFrameRotation();
    }

    @VisibleForTesting
    void setDisplayRotation(int displayRotation) {
        mDisplayRotation = displayRotation;
        refreshPreprocessFrameRotation();
    }

    public void setCameraParameters(int sensorRotation) {
        if (DBG) Log.d(TAG, "setCameraParameters() called with: " +  "sensorRotation = [" + sensorRotation + "]");
        mCameraSensorRotation = sensorRotation;
        refreshPreprocessFrameRotation();
    }

    @WorkAreaOrientation
    @Override
    public int getNativeDisplayRotation() {
        int rotation = mDisplayRotation;
        if (mNaturalOrientationIsLandscape) {
            rotation = (360 + rotation + LANDSCAPE_ORIENTATION_CORRECTION) % 360;
        }

        switch (rotation) {
            case 0:
                return WORK_AREA_ORIENTATION_PORTAIT;
            case 90:
                return WORK_AREA_ORIENTATION_LANDSCAPE_RIGHT;
            case 180:
                return WORK_AREA_ORIENTATION_PORTAIT_UPSIDE_DOWN;
            case 270:
                return WORK_AREA_ORIENTATION_LANDSCAPE_LEFT;
            default:
                throw new IllegalStateException();
        }
    }

    private void refreshPreprocessFrameRotation() {
        int rotation = DisplayHelper.getCameraRotationToNatural(mDisplayRotation, mCameraSensorRotation, false);

        int nativeDisplayRotation = getNativeDisplayRotation();
        if (nativeDisplayRotation == WORK_AREA_ORIENTATION_LANDSCAPE_RIGHT
                || nativeDisplayRotation == WORK_AREA_ORIENTATION_LANDSCAPE_LEFT) {
            rotation = (360 + rotation - 90) % 360;
        }
        mPreprocessFrameRotation = rotation;
        if (DBG) Log.v(TAG, "refreshPreprocessFrameRotation() rotation result: " + mPreprocessFrameRotation);
    }

    @SuppressLint("Range")
    @Override
    public int getPreprocessFrameRotation(int frameWidth, int frameHeight) {
        if (!sanityCheckPreprocessFrameRotation(frameWidth, frameHeight, mPreprocessFrameRotation)) {
            if (DBG) Log.v(TAG, "Skipping frame due to orientation inconsistency."
                    + " Frame size: " + frameWidth + "x" + frameHeight
                    + "; " + this.toString()
            );
            return -1;
        }
        return mPreprocessFrameRotation;
    }

    private boolean sanityCheckPreprocessFrameRotation(int frameWidth, int frameHeight, int rotation) {
        boolean isPortraitFrame = frameHeight >= frameWidth;
        boolean orientationChanged = rotation == 90 || rotation == 270;

        // Destination frame must be 720x1280
        //noinspection RedundantIfStatement
        if ((isPortraitFrame && orientationChanged) || (!isPortraitFrame && !orientationChanged)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "DisplayConfigurationImpl{" +
                "mCameraSensorRotation=" + mCameraSensorRotation +
                ", mDisplayRotation=" + mDisplayRotation +
                ", mNaturalOrientationIsLandscape=" + mNaturalOrientationIsLandscape +
                ", mPreprocessFrameRotation=" + mPreprocessFrameRotation +
                '}';
    }
}
