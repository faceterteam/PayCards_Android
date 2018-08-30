package cards.pay.paycardsrecognizer.sdk.ndk;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Display;

class RecognitionCoreDummy implements RecognitionCoreImpl {

    private final Rect mCardFrameRect = new Rect(30, 432, 30+660, 432 + 416);

    public void deploy() {
    }

    @Override
    public void setStatusListener(@Nullable RecognitionStatusListener listener) {
    }

    @Override
    public void setTorchStatus(boolean isTurnedOn) {
    }

    @Override
    public void setTorchListener(@Nullable TorchStatusListener listener) {
    }

    @Override
    public void setRecognitionMode(int mode) {
    }

    @Override
    public void setDisplayConfiguration(@NonNull DisplayConfiguration configuration) {
    }

    @Override
    public Rect getCardFrameRect() {
        return mCardFrameRect;
    }

    @Override
    public int processFrameYV12(int width, int height, byte[] buffer) {
        return 0;
    }

    @Override
    public void resetResult() {
    }

    @Override
    public void setIdle(boolean isIdle) {
    }

    @Override
    public boolean isIdle() {
        return true;
    }
}
