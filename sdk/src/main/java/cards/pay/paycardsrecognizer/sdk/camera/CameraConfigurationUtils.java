package cards.pay.paycardsrecognizer.sdk.camera;

import android.graphics.Rect;
import android.hardware.Camera;
import android.support.annotation.RestrictTo;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cards.pay.paycardsrecognizer.sdk.utils.Constants;

import static android.hardware.Camera.Area;
import static android.hardware.Camera.CameraInfo;
import static android.hardware.Camera.getCameraInfo;
import static android.hardware.Camera.getNumberOfCameras;
import static android.hardware.Camera.open;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class CameraConfigurationUtils {

    private static final float MAX_EXPOSURE_COMPENSATION = 1.5f;
    private static final float MIN_EXPOSURE_COMPENSATION = 0.0f;

    private static final boolean DBG = Constants.DEBUG;
    private static final String TAG = "CameraConfig";

    private static final boolean ENABLE_EXPOSURE = false;
    private static final boolean ENABLE_METERING = false;


    private CameraConfigurationUtils() {}

    static Camera createCamera() {
        final int numberOfCameras = getNumberOfCameras();
        final CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            getCameraInfo(i, cameraInfo);
            if (CameraInfo.CAMERA_FACING_BACK == cameraInfo.facing) {
                return open(i);
            }
        }
        if (0 < numberOfCameras) {
            return open(0);
        }
        return null;
    }

    static boolean isFlashSupported(final Camera camera) {
        final Camera.Parameters params = camera.getParameters();
        return params.getSupportedFlashModes() != null;
    }

    static boolean setFlashLight(final Camera camera, final boolean enableFlash) {
        final Camera.Parameters params = camera.getParameters();
        final List<String> flashes = params.getSupportedFlashModes();
        if (null != flashes) {
            if (enableFlash) {
                if (flashes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                } else if (flashes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                } else {
                    return false;
                }
            } else {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
        }
        setBestExposure(params, enableFlash);
        camera.setParameters(params);
        return enableFlash;
    }

    private static final List<String> FOCUS_LIST = Arrays.asList(
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
            Camera.Parameters.FOCUS_MODE_AUTO,
            Camera.Parameters.FOCUS_MODE_MACRO,
            Camera.Parameters.FOCUS_MODE_EDOF
    );

    public static void initAutoFocus(final Camera.Parameters cameraParameters) {
        initAutoFocus(cameraParameters, true);
    }

    public static void initAutoFocus(final Camera.Parameters cameraParameters, boolean enableContinuousFocus) {
        /*
        // FLASH_MODE_TORCH does not work with SCENE_MODE_BARCODE
        final List<String> supportedSceneModes = cameraParameters.getSupportedSceneModes();
        if (null != supportedSceneModes) {
            String resultSceneMode = null;
            if (supportedSceneModes.contains(Camera.Parameters.SCENE_MODE_BARCODE)) {
                resultSceneMode = Camera.Parameters.SCENE_MODE_BARCODE;
            }
            if (null != resultSceneMode) {
                cameraParameters.setSceneMode(resultSceneMode);
                return true;
            }
        } */

        final List<String> supportedFocusModes = cameraParameters.getSupportedFocusModes();
        if (null == supportedFocusModes) {
            return;
        }

        List<String> focusList = new ArrayList<>(FOCUS_LIST);
        if (!enableContinuousFocus) {
            focusList.remove(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            focusList.add(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        for (String focusMode : focusList) {
            if (supportedFocusModes.contains(focusMode)) {
                cameraParameters.setFocusMode(focusMode);
                break;
            }
        }
    }

    public static void initWhiteBalance(Camera.Parameters parameters) {
        final List<String> whiteBalance = parameters.getSupportedWhiteBalance();
        if (whiteBalance != null && whiteBalance.contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
            parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        }
    }


    public static void setBestExposure(Camera.Parameters parameters, final boolean lightOn) {
        if (!ENABLE_EXPOSURE) return;

        int minExposure = parameters.getMinExposureCompensation();
        int maxExposure = parameters.getMaxExposureCompensation();
        float step = parameters.getExposureCompensationStep();
        if ((minExposure != 0 || maxExposure != 0) && step > 0.0f) {
            // Set low when light is on
            float targetCompensation = lightOn ? MIN_EXPOSURE_COMPENSATION : MAX_EXPOSURE_COMPENSATION;
            int compensationSteps = Math.round(targetCompensation / step);
            float actualCompensation = step * compensationSteps;
            // Clamp value:
            compensationSteps = Math.max(Math.min(compensationSteps, maxExposure), minExposure);
            if (parameters.getExposureCompensation() == compensationSteps) {
                if (DBG) Log.i("CameraConfig", "Exposure compensation already set to " + compensationSteps + " / " + actualCompensation);
            } else {
                if (DBG) Log.i("CameraConfig", "Setting exposure compensation to " + compensationSteps + " / " + actualCompensation);
                parameters.setExposureCompensation(compensationSteps);
            }
        } else {
            if (DBG) Log.i("CameraConfig", "Camera does not support exposure compensation");
        }
    }

    public static void setMetering(Camera.Parameters parameters) {
        if (!ENABLE_METERING) return;
        if (parameters.isVideoStabilizationSupported()) {
            parameters.setVideoStabilization(false);
        }

        setFocusArea(parameters);
        setMeteringArea(parameters);
    }

    public static void setFocusArea(Camera.Parameters parameters) {
        if (parameters.getMaxNumFocusAreas() > 0) {
            Log.i(TAG, "Old focus areas: " + toString(parameters.getFocusAreas()));
            List<Camera.Area> cardArea = buildCardArea();
            Log.i(TAG, "Setting focus area to : " + toString(cardArea));
            parameters.setFocusAreas(cardArea);
        } else {
            Log.i(TAG, "Device does not support focus areas");
        }
    }

    public static void setMeteringArea(Camera.Parameters parameters) {
        if (parameters.getMaxNumMeteringAreas() > 0) {
            Log.i(TAG, "Old metering areas: " + parameters.getMeteringAreas());
            List<Camera.Area> cardArea = buildCardArea();
            Log.i(TAG, "Setting metering area to : " + toString(cardArea));
            parameters.setMeteringAreas(cardArea);
        } else {
            Log.i(TAG, "Device does not support metering areas");
        }
    }

    private static List<Camera.Area> buildCardArea() {
        //Rect rect = new Rect(-917, 32, 917, 325);
        Rect rect = new Rect(-10, -10, 10, 10);
        return Collections.singletonList(new Area(rect, 1));
    }

    private static String toString(Iterable<Camera.Area> areas) {
        if (areas == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (Area area : areas) {
            result.append(area.rect).append(':').append(area.weight).append(' ');
        }
        return result.toString();
    }

}
