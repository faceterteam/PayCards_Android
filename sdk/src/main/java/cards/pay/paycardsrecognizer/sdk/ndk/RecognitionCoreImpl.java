package cards.pay.paycardsrecognizer.sdk.ndk;

import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface RecognitionCoreImpl {

    void setStatusListener(@Nullable RecognitionStatusListener listener);

    void setTorchStatus(boolean isTurnedOn);

    void setTorchListener(@Nullable TorchStatusListener listener);

    void setRecognitionMode(@RecognitionConstants.RecognitionMode int mode);

    void setDisplayConfiguration(@NonNull DisplayConfiguration configuration);

    Rect getCardFrameRect();

    @RecognitionConstants.DetectedBorderFlags
    int processFrameYV12(int width, int height, byte[] buffer);

    void resetResult();

    void setIdle(boolean isIdle);

    boolean isIdle();

}
