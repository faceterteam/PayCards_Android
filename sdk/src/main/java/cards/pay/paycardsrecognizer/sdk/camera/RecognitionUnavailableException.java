package cards.pay.paycardsrecognizer.sdk.camera;

public final class RecognitionUnavailableException extends Exception {

    public static final int ERROR_OTHER = 0;

    public static final int ERROR_NO_CAMERA = 1;

    public static final int ERROR_OLD_DEVICE = 2;

    public static final int ERROR_CAMERA_NOT_SUPPORTED = 3;

    public static final int ERROR_NO_CAMERA_PERMISSION = 4;

    public static final int ERROR_UNSUPPORTED_ARCHITECTURE = 5;

    public final int errorCode;

    public RecognitionUnavailableException() {
        errorCode = ERROR_OTHER;
    }

    public RecognitionUnavailableException(int errorCode) {
        this.errorCode = errorCode;
    }

    public RecognitionUnavailableException(String detailMessage) {
        super(detailMessage);
        errorCode = ERROR_OTHER;
    }

    @Override
    public String getMessage() {
        switch (errorCode) {
            case ERROR_NO_CAMERA:
                return "No camera";
            case ERROR_OLD_DEVICE:
                return "Device is considered being too old for smooth camera experience, so camera will not be used.";
            case ERROR_NO_CAMERA_PERMISSION:
                return "No camera permission";
            case ERROR_CAMERA_NOT_SUPPORTED:
                return "Camera not supported";
            case ERROR_UNSUPPORTED_ARCHITECTURE:
                return "Unsupported architecture";
            default:
                return super.getMessage();
        }
    }
}
