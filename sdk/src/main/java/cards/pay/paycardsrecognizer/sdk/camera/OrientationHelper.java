package cards.pay.paycardsrecognizer.sdk.camera;

import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import cards.pay.paycardsrecognizer.sdk.BuildConfig;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class OrientationHelper {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "OrientationHelper";

    private OrientationHelper() {}

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

    public static int getDisplayRotationDegrees(Display display) {
        return getDisplayRotationDegrees(display.getRotation());
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

    @SuppressWarnings("SuspiciousNameCombination")
    public static Rect rotateRect(Rect src, int width, int height, int degrees, @Nullable Rect dst) {
        if (dst == null) dst = new Rect();
        int rotation = (degrees >= 0 ? degrees : (360 - degrees));

        int offset1 = src.left;
        int offset2 = src.top;
        int offset3 = width - src.right;
        int offset4 = height - src.bottom;

        if (rotation == 0) {
            dst.set(src.left, src.top, src.right, src.bottom);
        } else if (rotation == 90) {
            dst.set(offset2, offset3, offset2 + src.height(), offset3 + src.width());
        } else if (rotation == 180) {
            dst.set(offset3, offset4, offset3 + src.width(), offset4 + src.height());
        } else if (rotation == 270) {
            dst.set(offset4, offset1, offset4 + src.height(), offset1 + src.width());
        }

        if (DBG) Log.v(TAG, "rotateRect() degrees: " + degrees + "src: " + src.toString() + "; res: " + dst.toString());
        return dst;
    }
}
