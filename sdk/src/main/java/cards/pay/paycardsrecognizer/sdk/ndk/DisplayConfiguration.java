package cards.pay.paycardsrecognizer.sdk.ndk;

import androidx.annotation.IntRange;

import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.WorkAreaOrientation;

public interface DisplayConfiguration {
    @WorkAreaOrientation
    int getNativeDisplayRotation();

    @IntRange(from=0, to=360)
    int getPreprocessFrameRotation(int width, int height);
}
