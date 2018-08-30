package cards.pay.paycardsrecognizer.sdk.ndk;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.Display;

import java.io.IOException;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class RecognitionCore {

    private static volatile RecognitionCore sInstance;

    private static volatile RecognitionCoreImpl sImpl = new RecognitionCoreDummy();

    public static RecognitionCore getInstance(Context context) {
        try {
            deploy(context);
        } catch (IOException | UnsatisfiedLinkError e) {
            Log.e("RecognitionCore", "initialization failed", e);
        }
        return sInstance;
    }

    public static void deploy(Context context) throws IOException, UnsatisfiedLinkError {
        if (sInstance == null) {
            synchronized (RecognitionCore.class) {
                if (sInstance == null) {
                    try {
                        RecognitionCoreNdk ndkImpl = RecognitionCoreNdk.getInstance(context.getApplicationContext());
                        ndkImpl.deploy();
                        sImpl = ndkImpl;
                    } finally {
                        sInstance = new RecognitionCore();
                    }
                }
            }
        }
    }

    public static boolean isInitialized() {
        synchronized (RecognitionCore.class) {
            return sInstance != null;
        }
    }

    public boolean isDeviceSupported() {
        return !(sImpl instanceof RecognitionCoreDummy);
    }

    public void setStatusListener(@Nullable RecognitionStatusListener listener) {
        sImpl.setStatusListener(listener);
    }

    public void setTorchStatus(boolean isTurnedOn) {
        sImpl.setTorchStatus(isTurnedOn);
    }

    public void setTorchListener(@Nullable TorchStatusListener listener) {
        sImpl.setTorchListener(listener);
    }

    public void setRecognitionMode(@RecognitionConstants.RecognitionMode int mode) {
        sImpl.setRecognitionMode(mode);
    }

    public void setDisplayConfiguration(@NonNull DisplayConfiguration configuration) {
        sImpl.setDisplayConfiguration(configuration);
    }

    public Rect getCardFrameRect() {
        return sImpl.getCardFrameRect();
    }

    @RecognitionConstants.DetectedBorderFlags
    public int processFrameYV12(int width, int height, byte buffer[]) {
        return sImpl.processFrameYV12(width, height, buffer);
    }

    public void resetResult() {
        sImpl.resetResult();
    }

    public void setIdle(boolean isIdle) {
        sImpl.setIdle(isIdle);
    }

    public boolean isIdle() {
        return sImpl.isIdle();
    }
}
