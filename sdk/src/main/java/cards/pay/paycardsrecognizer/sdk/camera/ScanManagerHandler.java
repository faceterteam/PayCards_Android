package cards.pay.paycardsrecognizer.sdk.camera;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RestrictTo;
import android.util.Log;

import java.lang.ref.WeakReference;

import cards.pay.paycardsrecognizer.sdk.utils.Constants;


/**
 * Custom message handler for main UI thread.
 * <p>
 * Receives messages from the renderer thread with UI-related updates, like the camera
 * parameters (which we show in a text message on screen).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ScanManagerHandler extends Handler {
    private static final boolean DBG = Constants.DEBUG;
    private static final String TAG = "ScanManagerHandler";

    private static final int MSG_SEND_OPEN_CAMERA_ERROR = 1;

    private static final int MSG_SEND_RENDER_THREAD_ERROR = 2;

    private static final int MSG_SEND_CAMERA_OPENED = 3;

    private static final int MSG_SEND_NEW_BORDERS = 4;

    private static final int MSG_SEND_FPS_REPORT = 5;

    private static final int MSG_SEND_AUTO_FOCUS_MOVING = 6;

    private static final int MSG_SEND_AUTO_FOCUS_COMPLETE = 7;

    private WeakReference<ScanManager> mWeakScanManager;

    public ScanManagerHandler(ScanManager manager) {
        mWeakScanManager = new WeakReference<>(manager);
    }

    public void sendOpenCameraError(Exception e) {
        sendMessage(obtainMessage(MSG_SEND_OPEN_CAMERA_ERROR, e));
    }

    public void sendRenderThreadError(Throwable e) {
        sendMessage(obtainMessage(MSG_SEND_RENDER_THREAD_ERROR, e));
    }

    public void sendCameraOpened(Camera.Parameters cameraParameters) {
        sendMessage(obtainMessage(MSG_SEND_CAMERA_OPENED, cameraParameters));
    }

    public void sendFrameProcessed(int newBorders) {
        sendMessage(obtainMessage(MSG_SEND_NEW_BORDERS, newBorders, 0));
    }

    public void sendFpsResport(String fpSreport) {
        sendMessage(obtainMessage(MSG_SEND_FPS_REPORT, fpSreport));
    }

    public void sendAutoFocusMoving(boolean isStart, String cameraFocusMode) {
        sendMessage(obtainMessage(MSG_SEND_AUTO_FOCUS_MOVING, isStart ? 1 : 0, 0, cameraFocusMode));
    }

    public void sendAutoFocusComplete(boolean isSuccess, String cameraFocusMode) {
        sendMessage(obtainMessage(MSG_SEND_AUTO_FOCUS_COMPLETE, isSuccess ? 1 : 0, 0, cameraFocusMode));
    }

    @Override
    public void handleMessage(Message msg) {
        ScanManager scanManager = mWeakScanManager.get();
        if (scanManager == null) {
            if (DBG) Log.d(TAG, "Got message for dead activity");
            return;
        }

        switch (msg.what) {
            case MSG_SEND_OPEN_CAMERA_ERROR:
                scanManager.onOpenCameraError((Exception) msg.obj);
                break;
            case MSG_SEND_RENDER_THREAD_ERROR:
                scanManager.onRenderThreadError((Throwable) msg.obj);
                break;
            case MSG_SEND_CAMERA_OPENED:
                scanManager.onCameraOpened((Camera.Parameters) msg.obj);
                break;
            case MSG_SEND_NEW_BORDERS:
                scanManager.onFrameProcessed(msg.arg1);
                break;
            case MSG_SEND_FPS_REPORT:
                scanManager.onFpsReport((String)msg.obj);
                break;
            case MSG_SEND_AUTO_FOCUS_MOVING:
                scanManager.onAutoFocusMoving(msg.arg1 != 0, (String)msg.obj);
                break;
            case MSG_SEND_AUTO_FOCUS_COMPLETE:
                scanManager.onAutoFocusComplete(msg.arg1 != 0, (String)msg.obj);
                break;
            default:
                throw new RuntimeException("Unknown message " + msg.what);
        }
    }
}
