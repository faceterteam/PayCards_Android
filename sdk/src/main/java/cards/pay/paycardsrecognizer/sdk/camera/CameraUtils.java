package cards.pay.paycardsrecognizer.sdk.camera;

import android.hardware.Camera;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;
import android.view.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cards.pay.paycardsrecognizer.sdk.utils.Size;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class CameraUtils {

    public static final NativeSupportedSize CAMERA_RESOLUTION = NativeSupportedSize.RESOLUTION_1280_X_720;

    private static NativeSupportedSize sBestCameraPreviewSize;

    public static boolean isCameraSupported() throws BlockingOperationException {
        if (sBestCameraPreviewSize != null) {
            return sBestCameraPreviewSize != NativeSupportedSize.RESOLUTION_NO_CAMERA;
        }
        throw new BlockingOperationException();
    }

    public static boolean isCameraSupportedBlocking() {
        try {
            return isCameraSupported();
        } catch (BlockingOperationException ignore) {
        }

        // XXX slooow
        generateBestCameraPreviewSize();

        return sBestCameraPreviewSize != null && sBestCameraPreviewSize != NativeSupportedSize.RESOLUTION_NO_CAMERA;
    }

    @Nullable
    public static NativeSupportedSize findBestCameraSupportedSize(final Iterable<Camera.Size> previewSizes) {
        NativeSupportedSize best = NativeSupportedSize.RESOLUTION_NO_CAMERA;

        if (null == previewSizes) {
            return NativeSupportedSize.RESOLUTION_NO_CAMERA;
        }

        for (final Camera.Size previewSize : previewSizes) {
            for (final NativeSupportedSize supportedSize : NativeSupportedSize.values()) {
                if (previewSize.width == supportedSize.size.width && previewSize.height == supportedSize.size.height) {
                    if (supportedSize.compareTo(best) < 0) {
                        best = supportedSize;
                    }
                    break;
                }
            }
        }

        return best;
    }

    public static void generateBestCameraPreviewSize() {
        if (null == sBestCameraPreviewSize) {
            try {
                final Camera camera = Camera.open();
                if(null != camera) {
                    generateBestCameraPreviewSize(camera, camera.getParameters().getSupportedPreviewSizes());
                    camera.release();
                }
            }
            catch (final Exception ignored) {
                // pass
            }
        }
    }

    static void generateBestCameraPreviewSize(final Camera camera, final Iterable<Camera.Size> previewSizes) {
        if (null == previewSizes) {
            sBestCameraPreviewSize = NativeSupportedSize.RESOLUTION_NO_CAMERA;
            return;
        }

        sBestCameraPreviewSize = findBestCameraSupportedSize(previewSizes);

        if (sBestCameraPreviewSize == NativeSupportedSize.RESOLUTION_NO_CAMERA) {
            //trying to set all items...
            for (final NativeSupportedSize nativeSupportedSize : NativeSupportedSize.values()) {
                if (tryToSetCameraSize(camera, nativeSupportedSize)) {
                    sBestCameraPreviewSize = nativeSupportedSize;
                    return;
                }
            }
            sBestCameraPreviewSize = NativeSupportedSize.RESOLUTION_NO_CAMERA;
        }
    }

    private static boolean tryToSetCameraSize(final Camera camera, final NativeSupportedSize nativeSupportedSize) {
        if (null == camera) {
            return false;
        }
        final Camera.Parameters params = camera.getParameters();

        params.setPreviewSize(nativeSupportedSize.size.width, nativeSupportedSize.size.height);

        try {
            camera.setParameters(params);
            final Camera.Size previewSize = camera.getParameters().getPreviewSize();
            return !(previewSize.width != nativeSupportedSize.size.width && previewSize.height != nativeSupportedSize.size.height);
        }
        catch (final Exception ignored) {
            return false;
        }
    }

    public static String getSupportedSizesDescription(List<Camera.Size> sizes) {
        List<String> text = new ArrayList<>(sizes.size());
        for (Camera.Size size: sizes) text.add(String.format(Locale.US, "[%dx%d]", size.width, size.height));
        return TextUtils.join(", ", text);
    }

    @Nullable
    public static Camera.CameraInfo getBackCameraInfo() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return cameraInfo;
            }
        }
        return null;
    }

    public static int getBackCameraSensorOrientation() {
        Camera.CameraInfo cameraInfo = getBackCameraInfo();
        return cameraInfo == null ? 0 : cameraInfo.orientation;
    }

    public static int getBackCameraDataRotation(Display display) {
        return getCameraDataRotation(display, getBackCameraInfo());
    }

    // http://www.wordsaretoys.com/2013/10/25/roll-that-camera-zombie-rotation-and-coversion-from-yv12-to-yuv420planar/
    private static int getCameraDataRotation(Display display, @Nullable Camera.CameraInfo cameraInfo) {
        int rotation = OrientationHelper.getDisplayRotationDegrees(display);
        if (cameraInfo == null) return 0;
        return OrientationHelper.getCameraRotationToNatural(rotation, cameraInfo.orientation, cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
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
