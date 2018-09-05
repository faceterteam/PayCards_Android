package cards.pay.paycardsrecognizer.sdk.ndk;

import android.support.annotation.IntDef;
import android.support.annotation.RestrictTo;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class RecognitionConstants {

    public static final int DETECTED_BORDER_TOP =   1;
    public static final int DETECTED_BORDER_BOTTOM = 1 << 1;
    public static final int DETECTED_BORDER_LEFT =   1 << 2;
    public static final int DETECTED_BORDER_RIGHT =  1 << 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true,
            value = {DETECTED_BORDER_TOP, DETECTED_BORDER_BOTTOM, DETECTED_BORDER_LEFT, DETECTED_BORDER_RIGHT})
    public @interface DetectedBorderFlags {}


    public static final int RECOGNIZER_MODE_NUMBER = 1;
    public static final int RECOGNIZER_MODE_DATE = 1 << 1;
    public static final int RECOGNIZER_MODE_NAME = 1 << 2;
    public static final int RECOGNIZER_MODE_GRAB_CARD_IMAGE = 1 << 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true,
            value = {RECOGNIZER_MODE_NUMBER, RECOGNIZER_MODE_DATE, RECOGNIZER_MODE_NAME, RECOGNIZER_MODE_GRAB_CARD_IMAGE})
    public @interface RecognitionMode {}


    static final int WORK_AREA_ORIENTATION_UNKNOWN = 0;
    static final int WORK_AREA_ORIENTATION_PORTAIT = 1;
    static final int WORK_AREA_ORIENTATION_PORTAIT_UPSIDE_DOWN = 2;
    static final int WORK_AREA_ORIENTATION_LANDSCAPE_RIGHT = 3;
    static final int WORK_AREA_ORIENTATION_LANDSCAPE_LEFT = 4;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {WORK_AREA_ORIENTATION_UNKNOWN,
            WORK_AREA_ORIENTATION_PORTAIT,
            WORK_AREA_ORIENTATION_PORTAIT_UPSIDE_DOWN,
            WORK_AREA_ORIENTATION_LANDSCAPE_RIGHT,
            WORK_AREA_ORIENTATION_LANDSCAPE_LEFT})
    @interface WorkAreaOrientation {}

    private RecognitionConstants() {
    }
}
