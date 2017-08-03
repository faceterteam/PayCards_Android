package cards.pay.paycardsrecognizer.sdk.ndk;

import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface TorchStatusListener {

    void onTorchStatusChanged(boolean turnTorchOn);

}
