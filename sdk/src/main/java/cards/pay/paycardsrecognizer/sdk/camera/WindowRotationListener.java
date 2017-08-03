package cards.pay.paycardsrecognizer.sdk.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RestrictTo;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;

import cards.pay.paycardsrecognizer.sdk.utils.Constants;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class WindowRotationListener {

    private static final String TAG = "WindowRotationListener";
    private static boolean DBG = Constants.DEBUG;

    public interface RotationListener {
        void onWindowRotationChanged();
    }

    private interface Impl {
        void register(Context context, Display display, RotationListener listener);
        void unregister();
    }

    private static final Impl sImpl;

    static {
        if (Build.VERSION.SDK_INT >= 17) {
            sImpl = new ImplApi17();
        } else {
            sImpl = new ImplDefault();
        }
    }

    public void register(Context context, Display display, RotationListener listener) {
        sImpl.register(context, display, listener);
    }

    public void unregister() {
        sImpl.unregister();
    }

    private static class ImplDefault implements Impl {

        private Display mDisplay;

        private RotationListener mListener;

        private OrientationEventListener mOrientationListener;

        @Override
        public void register(Context context, Display display, RotationListener listener) {
            mDisplay = display;
            mListener = listener;
            mOrientationListener = new OrientationEventListener(context) {
                int lastWindowOrientation = OrientationHelper.getDisplayRotationDegrees(mDisplay);
                int lastOrientation = lastWindowOrientation;

                @Override
                public void onOrientationChanged(int orientation) {
                    if (mDisplay == null) return; // onOrientationChanged() called after unsubscribe. Skip
                    orientation = ((orientation + 45) / 90) * 90;
                    if (orientation == lastOrientation) return;
                    lastOrientation = orientation;

                    int windowOrientation = OrientationHelper.getDisplayRotationDegrees(mDisplay);
                    if (windowOrientation == lastWindowOrientation) return;
                    lastWindowOrientation = windowOrientation;

                    if (orientation == ORIENTATION_UNKNOWN) return;
                    if (mListener != null) mListener.onWindowRotationChanged();
                }
            };
            mOrientationListener.enable();
        }

        @Override
        public void unregister() {
            if (mOrientationListener == null) return;
            mOrientationListener.disable();
            mOrientationListener = null;
            mDisplay = null;
            mListener = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static class ImplApi17 implements Impl, DisplayManager.DisplayListener {

        private RotationListener mListener;

        private final Handler mHandler;

        private DisplayManager mDisplayManager;

        private int mDisplayId;

        public ImplApi17() {
            mHandler = new Handler();
        }

        @Override
        public void register(Context context, Display display, RotationListener listener) {
            mListener = listener;
            mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            mDisplayManager.registerDisplayListener(this, mHandler);
            mDisplayId = display.getDisplayId();
        }

        @Override
        public void unregister() {
            if (mDisplayManager == null) return;
            mDisplayManager.unregisterDisplayListener(this);
            mDisplayManager = null;
            mListener = null;
        }

        @Override
        public void onDisplayAdded(int displayId) {
            if (DBG) Log.d(TAG, "onDisplayAdded() called with: " +  "displayId = [" + displayId + "]");
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            if (DBG) Log.d(TAG, "onDisplayRemoved() called with: " +  "displayId = [" + displayId + "]");
        }

        @Override
        public void onDisplayChanged(int displayId) {
            if (DBG) Log.d(TAG, "onDisplayChanged() called with: " +  "displayId = [" + displayId + "]");
            if (mListener != null && displayId == this.mDisplayId) {
                mListener.onWindowRotationChanged();
            }
        }
    }
}
