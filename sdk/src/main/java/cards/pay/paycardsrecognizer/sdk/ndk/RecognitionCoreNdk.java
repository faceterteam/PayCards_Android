package cards.pay.paycardsrecognizer.sdk.ndk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.view.Display;

import java.io.IOException;

import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.DetectedBorderFlags;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RecognitionMode;

final class RecognitionCoreNdk implements RecognitionCoreImpl {

    private static final int MESSAGE_RESULT_RECEIVED = 1;

    private static final int MESSAGE_CARD_IMAGE_RECEIVED = 2;

    @SuppressLint("StaticFieldLeak")
    private static volatile RecognitionCoreNdk sInstance;

    public static RecognitionCoreNdk getInstance(Context context) {
        if (sInstance == null) sInstance = new RecognitionCoreNdk(context.getApplicationContext());
        return sInstance;
    }

    static {
        System.loadLibrary("c++_shared");
        System.loadLibrary("cardrecognizer");
    }

    private final Context mAppContext;

    private final Handler mMainThreadHandler;

    private final Rect mCardFrameRect = new Rect(30, 432, 30+660, 432 + 416);

    private DisplayConfiguration mDisplayConfiguration = new DisplayConfigurationImpl();

    @Nullable
    private RecognitionStatusListener mStatusListener;

    @Nullable
    private TorchStatusListenerHandler mTorchStatusListener;

    private RecognitionCoreNdk(Context appContext) {
        nativeInit();
        mAppContext = appContext.getApplicationContext();

        try {
            deploy();
        } catch (IOException e) {
            Log.e("CardRecognizerCore", "initialization failed", e);
        }

        mMainThreadHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_RESULT_RECEIVED:
                        if (mStatusListener != null) {
                            RecognitionResult result = (RecognitionResult)msg.obj;
                            mStatusListener.onRecognitionComplete(result);
                        }
                        return true;
                    case MESSAGE_CARD_IMAGE_RECEIVED:
                        if (mStatusListener != null) {
                            Bitmap bitmap = (Bitmap)msg.obj;
                            mStatusListener.onCardImageReceived(bitmap);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private static class TorchStatusListenerHandler extends Handler {

        private static final int MESSAGE_TORCH_STATUS_CHANGED = 3;

        private final TorchStatusListener mListener;

        public TorchStatusListenerHandler(Looper looper, TorchStatusListener listener) {
            super(looper);
            mListener = listener;
        }

        public TorchStatusListenerHandler(TorchStatusListener listener) {
            super();
            mListener = listener;
        }

        public void sendStatusChanged(boolean turnTorchOn) {
            removeMessages(MESSAGE_TORCH_STATUS_CHANGED);
            sendMessage(Message.obtain(this, MESSAGE_TORCH_STATUS_CHANGED, turnTorchOn ? 1 : 0, 0));
        }

        public void stop() {
            removeMessages(MESSAGE_TORCH_STATUS_CHANGED);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_TORCH_STATUS_CHANGED:
                    mListener.onTorchStatusChanged(msg.arg1 != 0);
                    break;
            }
            super.handleMessage(msg);
        }
    }

    public void deploy() throws IOException {
        NeuroDataHelper dataHelper = new NeuroDataHelper(mAppContext);
        dataHelper.unpackAssets();
        nativeSetDataPath(dataHelper.getDataBasePath().getAbsolutePath());
        nativeDeploy();
    }

    public void setStatusListener(@Nullable RecognitionStatusListener listener) {
        this.mStatusListener = listener;
    }

    public void setTorchStatus(boolean isTurnedOn) {
        nativeSetTorchStatus(isTurnedOn);
    }

    public void setTorchListener(@Nullable TorchStatusListener listener) {
        synchronized (this) {
            if (mTorchStatusListener != null && mTorchStatusListener.mListener == listener) {
                return;
            }
            if (mTorchStatusListener != null) {
                mTorchStatusListener.stop();
                mTorchStatusListener = null;
            }
            if (listener != null) {
                mTorchStatusListener = new TorchStatusListenerHandler(Looper.myLooper(), listener);
            }
        }
    }

    public synchronized void setRecognitionMode(@RecognitionMode int mode) {
        nativeSetRecognitionMode(mode);
    }

    public synchronized void setDisplayConfiguration(@NonNull DisplayConfiguration configuration) {
        this.mDisplayConfiguration = configuration;
        nativeSetOrientation(mDisplayConfiguration.getNativeDisplayRotation());
        nativeCalcWorkingArea(1280, 720, 32, mCardFrameRect);
    }

    public Rect getCardFrameRect() {
        return mCardFrameRect;
    }

    @DetectedBorderFlags
    public synchronized int processFrameYV12(int width, int height, byte buffer[]) {
        int orientation = mDisplayConfiguration.getPreprocessFrameRotation(width, height);
        if (orientation == -1) return 0;

        return nativeProcessFrameYV12(width, height, orientation, buffer);
    }

    public void resetResult() {
        nativeResetResult();
    }

    public void setIdle(boolean isIdle) {
        nativeSetIdle(isIdle);
    }

    public boolean isIdle() {
        return nativeIsIdle();
    }

    // Called from native thread.
    @Keep
    @WorkerThread
    private static void onRecognitionResultReceived(
            boolean isFirst,
            boolean isFinal,
            String number, String date, String name, String nameRaw,
                                                    Bitmap cardImage,
                                                    int numberRectX, int numberRectY, int numberRectWidth, int numberRectHeight
    ) {
        final Rect numberRect;

        if (sInstance == null) return;

        if (numberRectWidth != 0 && numberRectHeight != 0) {
            numberRect = new Rect(numberRectX, numberRectY, numberRectX + numberRectWidth, numberRectY + numberRectHeight);
        } else {
            numberRect = null;
        }

        RecognitionResult result = new RecognitionResult.Builder()
                .setIsFirst(isFirst)
                .setIsFinal(isFinal)
                .setNumber(number)
                .setName(name)
                .setDate(date)
                .setNameRaw(nameRaw)
                .setNumberImageRect(numberRect)
                .setCardImage(cardImage)
                .build();

        Message msg = Message.obtain(sInstance.mMainThreadHandler, MESSAGE_RESULT_RECEIVED, result);
        msg.sendToTarget();
    }

    // Called from native thread.
    @Keep
    @WorkerThread
    private static void onCardImageReceived(Bitmap cardImage) {
        Message msg = Message.obtain(sInstance.mMainThreadHandler, MESSAGE_CARD_IMAGE_RECEIVED, cardImage);
        msg.sendToTarget();
    }

    @Keep
    @WorkerThread
    // Called from native thread.
    private static void onTorchStatusChanged(boolean status) {
        /// of turn torch on
        synchronized (RecognitionCoreNdk.class) {
            if (sInstance == null) return;
            synchronized (RecognitionCoreNdk.sInstance) {
                if (sInstance.mTorchStatusListener != null) {
                    sInstance.mTorchStatusListener.sendStatusChanged(status);
                }
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        nativeDestroy();
        super.finalize();
    }

    static native void nativeInit();

    static native void nativeDestroy();

    native void nativeSetDataPath(String path);

    native void nativeDeploy();

    native void nativeSetRecognitionMode(@RecognitionMode int recognitionMode);

    native void nativeSetIdle(boolean idle);

    native void nativeSetTorchStatus(boolean isTurnedOn);

    native boolean nativeIsIdle();

    native void nativeCalcWorkingArea(int frameWidth, int frameHeight, int captureAreaWidth, Rect dstRect);

    native void nativeSetOrientation(int workAreaOrientation);

    native void nativeResetResult();

    @DetectedBorderFlags
    native int nativeProcessFrameYV12(int width, int height, int rotation, byte buffer[]);
}
