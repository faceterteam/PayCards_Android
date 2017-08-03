package cards.pay.paycardsrecognizer.sdk.ndk;


import android.graphics.Bitmap;
import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface RecognitionStatusListener {

    void onRecognitionComplete(RecognitionResult result);

    void onCardImageReceived(Bitmap bitmap);

}
