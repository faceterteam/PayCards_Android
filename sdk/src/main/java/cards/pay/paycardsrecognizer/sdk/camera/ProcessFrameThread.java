package cards.pay.paycardsrecognizer.sdk.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;

import java.util.Locale;

import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionCore;
import cards.pay.paycardsrecognizer.sdk.utils.Constants;

// https://github.com/googlesamples/android-vision/blob/master/visionSamples/barcode-reader/app/src/main/java/com/google/android/gms/samples/vision/barcodereader/ui/camera/CameraSource.java
class ProcessFrameThread extends Thread {

    private static final boolean DBG = Constants.DEBUG;
    private static final String TAG = "ProcessFrameThread";

    private final RecognitionCore mRecognitionCore;

    private final Camera mCamera;

    private final Callbacks mCallbacks;

    private final FpsCounter mFpsCounter;

    private final FpsCounter mDropFpsCounter;

    private final Object mLock = new Object();

    private boolean mActive = true;

    private volatile byte mPendingFrameData[];

    private int mFpsNo;

    int mPredBorders = 0;

    public interface Callbacks {
        void onFrameProcessed(int newBorders);
        void onFpsReport(String report);
    }

    /**
     * Marks the runnable as active/not active.  Signals any blocked threads to continue.
     */
    void setActive(boolean active) {
        synchronized (mLock) {
            if (active != mActive) {
                mActive = active;
                if (!mActive) {
                    mLock.notifyAll();
                } else {
                    if (mPendingFrameData != null) mLock.notifyAll();
                }
            }
        }
    }

    public void processFrame(byte[] data) {
        if (data == null) {
            throw new IllegalStateException();
        }
        synchronized (mLock) {
            if (mPendingFrameData != null) {
                mCamera.addCallbackBuffer(mPendingFrameData);
                mPendingFrameData = null;
                if (DBG) tickDropFps();
            }

            mPendingFrameData = data;

            // Notify the processor thread if it is waiting on the next frame (see below).
            mLock.notifyAll();
        }
    }

    public ProcessFrameThread(Context appContext, Camera camera, Callbacks callbacks) {
        super("ProcessFrameThread");
        mRecognitionCore = RecognitionCore.getInstance(appContext);
        mCamera = camera;
        mCallbacks = callbacks;
        if (DBG) {
            mFpsCounter = new FpsCounter();
            mDropFpsCounter = new FpsCounter();
        } else {
            mFpsCounter = null;
            mDropFpsCounter = null;
        }
    }

    /**
     * As long as the processing thread is active, this executes detection on frames
     * continuously.  The next pending frame is either immediately available or hasn't been
     * received yet.  Once it is available, we transfer the frame info to local variables and
     * run detection on that frame.  It immediately loops back for the next frame without
     * pausing.
     * <p>
     * If detection takes longer than the time in between new frames from the camera, this will
     * mean that this loop will run without ever waiting on a frame, avoiding any context
     * switching or frame acquisition time latency.
     * <p>
     * If you find that this is using more CPU than you'd like, you should probably decrease the
     * FPS setting above to allow for some idle time in between frames.
     */
    @Override
    public void run() {
        byte data[];

        if (DBG) Log.d(TAG, "Thread started. TID: " + Thread.currentThread().getId());

        WHILETRUE: while (true) {
            synchronized (mLock) {
                if (mPendingFrameData == null) {
                    try {
                        // Wait for the next frame to be received from the camera, since we
                        // don't have it yet.
                        mLock.wait();
                    } catch (InterruptedException e) {
                        if (DBG) Log.d(TAG, "Frame processing loop terminated.", e);
                        break WHILETRUE;
                    }
                }

                if (!mActive) {
                    // Exit the loop once this camera source is stopped or released.  We check
                    // this here, immediately after the wait() above, to handle the case where
                    // setActive(false) had been called, triggering the termination of this
                    // loop.
                    break WHILETRUE;
                }

                // Hold onto the frame data locally, so that we can use this for detection
                // below.  We need to clear mPendingFrameData to ensure that this buffer isn't
                // recycled back to the camera before we are done using that data.
                data = mPendingFrameData;
                mPendingFrameData = null;
            }

            if (DBG) tickFps(data);

            if (data == null) {
                if (DBG) Log.e(TAG, "data is null");
                throw new NullPointerException();
            }

            // The code below needs to run outside of synchronization, because this will allow
            // the camera to add pending frame(s) while we are running detection on the current
            // frame.
            int borders = mRecognitionCore.processFrameYV12(1280, 720, data);
            if (borders != mPredBorders) {
                mPredBorders = borders;
            }

            if (!mActive) {
                break WHILETRUE;
            }

            mCamera.addCallbackBuffer(data);
            mCallbacks.onFrameProcessed(borders);
        }

        if (DBG) Log.d(TAG, "Thread finished. TID: " + Thread.currentThread().getId());
    }

    private void tickFps(byte data[]) {
        mFpsCounter.tickFPS();
        mDropFpsCounter.update();
        nextFps();
        if (mFpsNo == 1) {
            if (DBG) Log.d(TAG, "onPreviewFrame() called with: " + "data.length: " + data.length
                    + "; thread: " + Thread.currentThread() + "; ");
        }
    }

    private void tickDropFps() {
        mDropFpsCounter.tickFPS();
        nextFps();
    }

    private void nextFps() {
        mFpsNo += 1;
        if (mFpsNo == 1) {
            mFpsCounter.setUpdateFPSFrames(50);
            mDropFpsCounter.setUpdateFPSFrames(50);
        } else {
            if (DBG && (mFpsNo % 20 == 0)) {
                mCallbacks.onFpsReport(String.format(Locale.US, "%s dropped: %.1f fps", mFpsCounter.toString(),
                        mDropFpsCounter.getLastFPS()));
            }
        }
    }
}
