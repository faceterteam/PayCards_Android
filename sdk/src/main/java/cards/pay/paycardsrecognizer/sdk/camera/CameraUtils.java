package cards.pay.paycardsrecognizer.sdk.camera;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.RestrictTo;

import cards.pay.paycardsrecognizer.sdk.utils.Size;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class CameraUtils {

    public static final NativeSupportedSize CAMERA_RESOLUTION = NativeSupportedSize.RESOLUTION_1280_X_720;

    public static boolean isCameraSupported(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    @SuppressWarnings("MagicNumber")
    public enum NativeSupportedSize {
        RESOLUTION_1280_X_720(1280, 720),
        RESOLUTION_NO_CAMERA(-1, -1);

        public final Size size;

        NativeSupportedSize(final int width, final int height) {
            this.size = new Size(width, height);
        }
    }

}
