package cards.pay.paycardsrecognizer.sdk.ndk;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import cards.pay.paycardsrecognizer.sdk.BuildConfig;

final class DisplayHelper {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "DisplayHelper";

    private DisplayHelper() {}

    /**
     * Returns the rotation of the screen with the rotation of the camera from its "natural" orientation.
     * @param displayRotation {@link Display#getRotation()} in degrees
     * @param cameraOrientation {@link android.hardware.Camera.CameraInfo#orientation} in degrees
     */
    // http://developer.android.com/intl/ru/reference/android/hardware/Camera.html#setDisplayOrientation%28int%29
    public static int getCameraRotationToNatural(int displayRotation, int cameraOrientation, boolean compensateMirror) {
        if (DBG) Log.d(TAG, "getCameraRotationToNatural() called with: " +  "displayRotation = [" + displayRotation + "], cameraOrientation = [" + cameraOrientation + "], compensateMirror = [" + compensateMirror + "]");
        int result = 0;
        if (compensateMirror) {
            result = (cameraOrientation + displayRotation) % 360;
            result = (360 - result) % 360;	// compensate the mirror
        } else {
            result = (cameraOrientation - displayRotation + 360) % 360;
        }
        return result;
    }

    public static int getDisplayRotationDegrees(int surfaceRotationVal) {
        switch (surfaceRotationVal) {
            case Surface.ROTATION_0: return 0;
            case Surface.ROTATION_90: return 90;
            case Surface.ROTATION_180: return 180;
            case Surface.ROTATION_270: return 270;
            default: throw new IllegalArgumentException();
        }
    }

    public static int getDisplayRotationDegrees(Display display) {return getDisplayRotationDegrees(display.getRotation());}

    public static boolean naturalOrientationIsLandscape(Display display) {
        int rotation = display.getRotation();

        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);

        switch (rotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                return dm.widthPixels > dm.heightPixels;
            case Surface.ROTATION_270:
            case Surface.ROTATION_90:
            default:
                return dm.heightPixels > dm.widthPixels;
        }
    }


}
