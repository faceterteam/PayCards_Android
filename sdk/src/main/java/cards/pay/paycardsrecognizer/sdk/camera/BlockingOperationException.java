package cards.pay.paycardsrecognizer.sdk.camera;

import androidx.annotation.RestrictTo;

import java.io.IOException;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class BlockingOperationException extends IOException {
    public BlockingOperationException() {
    }

    public BlockingOperationException(Throwable cause) {
        super(cause);
    }

    public BlockingOperationException(String detailMessage) {
        super(detailMessage);
    }

    public BlockingOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
