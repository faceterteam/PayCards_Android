package cards.pay.paycardsrecognizer.sdk.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RestrictTo;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.lang.ref.WeakReference;

import cards.pay.paycardsrecognizer.sdk.camera.gles.Drawable2d;
import cards.pay.paycardsrecognizer.sdk.camera.gles.EglCore;
import cards.pay.paycardsrecognizer.sdk.camera.gles.GlUtil;
import cards.pay.paycardsrecognizer.sdk.camera.gles.Sprite2d;
import cards.pay.paycardsrecognizer.sdk.camera.gles.Texture2dProgram;
import cards.pay.paycardsrecognizer.sdk.camera.gles.WindowSurface;
import cards.pay.paycardsrecognizer.sdk.utils.Constants;

/**
 * Thread that handles all rendering and camera operations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class RenderThread extends Thread {

    private static final boolean DBG = Constants.DEBUG;
    private static final String TAG = "RenderNCameraThread";

    // Object must be created on render thread to get correct Looper, but is used from
    // UI thread, so we need to declare it volatile to ensure the UI thread sees a fully
    // constructed object.
    private volatile RenderHandler mHandler;

    // Used to wait for the thread to start.
    private final Object mStartLock = new Object();
    private boolean mReady = false;

    private ScanManagerHandler mMainHandler;

    private final Context mAppContext;

    private CameraManager mCameraManager;

    // Receives the output from the camera preview.
    private SurfaceTexture mCameraTexture;

    private EglCore mEglCore;
    private WindowSurface mWindowSurface;

    private Texture2dProgram mTexProgram;
    private final Sprite2d mRect = new Sprite2d(new Drawable2d());

    // Orthographic projection matrix.
    private float[] mDisplayProjectionMatrix = new float[16];

    private int mWindowSurfaceWidth;
    private int mWindowSurfaceHeight;

    private int mCameraPreviewWidth = 1280;
    private int mCameraPreviewHeight = 720;
    private int mCameraRotation;

    private float mPosX, mPosY;

    private volatile boolean mOnFreeze;

    public RenderThread(Context context, ScanManagerHandler mainHandler) {
        mMainHandler = mainHandler;
        mAppContext = context.getApplicationContext();
    }

    /**
     * Thread entry point.
     */
    @Override
    public void run() {
        Looper.prepare();

        if (DBG) Log.d(TAG, "Thread started. TID: " + Thread.currentThread().getId());

        // We need to create the Handler before reporting ready.
        mHandler = new RenderHandler(this);
        synchronized (mStartLock) {
            mReady = true;
            mStartLock.notify();    // signal waitUntilReady()
        }

        try {
            // Prepare EGL and open the camera before we start handling messages.
            mEglCore = new EglCore(null, 0);

            mCameraManager = new CameraManager(mAppContext);
            mCameraManager.openCamera();
            mCameraManager.setProcessFrameCallbacks(new ProcessFrameThread.Callbacks() {
                @Override
                public void onFrameProcessed(int newBorders) {
                    mMainHandler.sendFrameProcessed(newBorders);
                }

                @Override
                public void onFpsReport(String report) {
                    mMainHandler.sendFpsResport(report);
                }
            });
            mCameraManager.setAutoFocusCallbacks(new AutoFocusManager.FocusMoveCallback() {
                @Override
                public void onAutoFocusMoving(boolean start, Camera camera) {
                    mMainHandler.sendAutoFocusMoving(start, camera.getParameters().getFocusMode());
                }

                @Override
                public void onAutoFocusComplete(boolean success, Camera camera) {
                    mMainHandler.sendAutoFocusComplete(success, camera.getParameters().getFocusMode());
                }
            });

            Camera.Size previewSize = mCameraManager.getCurrentPreviewSize();
            mCameraPreviewWidth = previewSize.width;
            mCameraPreviewHeight = previewSize.height;
            mCameraRotation = mCameraManager.calculateDataRotation();
            mMainHandler.sendCameraOpened(mCameraManager.getCamera().getParameters());
        } catch (Exception e) {
            Looper looper = Looper.myLooper();
            if (looper != null) looper.quit();

            if (mEglCore != null) mEglCore.release();

            mMainHandler.sendOpenCameraError(e);

            if (DBG) Log.d(TAG, "Thread finished. TID: " + Thread.currentThread().getId());
            synchronized (mStartLock) {
                mReady = false;
                mStartLock.notify();
            }
            return;
        }

        try {
            Looper.loop();
        } catch (Throwable e) {
            mMainHandler.sendRenderThreadError(e);
        } finally {
            if (DBG) Log.d(TAG, "looper quit");
            mCameraManager.releaseCamera();

            releaseGl();
            mEglCore.release();

            if (DBG) Log.d(TAG, "Thread finished. TID: " + Thread.currentThread().getId());
            synchronized (mStartLock) {
                mReady = false;
            }
        }
    }

    /**
     * Waits until the render thread is ready to receive messages.
     * <p>
     * Call from the UI thread.
     */
    public void waitUntilReady() {
        synchronized (mStartLock) {
            while (!mReady) {
                try {
                    mStartLock.wait();
                } catch (InterruptedException ie) { /* not expected */ }
            }
        }
    }

    /**
     * Shuts everything down.
     */
    private void shutdown() {
        if (DBG) Log.d(TAG, "shutdown()");
        mCameraManager.releaseCamera(); // avoid "sending message to a Handler on a dead thread"
        Looper.myLooper().quit();
    }

    /**
     * Returns the render thread's Handler.  This may be called from any thread.
     */
    public RenderHandler getHandler() {
        return mHandler;
    }

    /**
     * Handles the surface-created callback from SurfaceView.  Prepares GLES and the Surface.
     */
    private void surfaceAvailable(SurfaceHolder holder, boolean newSurface) {
        if (DBG) Log.d(TAG, "surfaceAvailable() called with: " +  "holder = [" + holder + "], newSurface = [" + newSurface + "]");
        mWindowSurface = new WindowSurface(mEglCore, holder, false);
        mWindowSurface.makeCurrent();

        // Create and configure the SurfaceTexture, which will receive frames from the
        // camera.  We set the textured rect's program to render from it.
        mTexProgram = new Texture2dProgram();
        int textureId = mTexProgram.createTextureObject();
        mCameraTexture = new SurfaceTexture(textureId);
        mRect.setTexture(textureId);

        if (!newSurface) {
            // This Surface was established on a previous run, so no surfaceChanged()
            // message is forthcoming.  Finish the surface setup now.
            //
            // We could also just call this unconditionally, and perhaps do an unnecessary
            // bit of reallocating if a surface-changed message arrives.
            mWindowSurfaceWidth = mWindowSurface.getWidth();
            mWindowSurfaceHeight = mWindowSurface.getHeight();
            finishSurfaceSetup();
        }

        mCameraTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                // SurfaceTexture.OnFrameAvailableListener; runs on arbitrary thread
                mHandler.sendFrameAvailable();
            }
        });
    }

    /**
     * Handles incoming frame of data from the camera.
     */
    private void frameAvailable() {
        mCameraTexture.updateTexImage();
        draw();
    }

    /**
     * Releases most of the GL resources we currently hold (anything allocated by
     * surfaceAvailable()).
     * <p>
     * Does not release EglCore.
     */
    private void releaseGl() {
        GlUtil.checkGlError("releaseGl start");

        if (mWindowSurface != null) {
            mWindowSurface.release();
            mWindowSurface = null;
        }
        if (mTexProgram != null) {
            mTexProgram.release();
            mTexProgram = null;
        }
        GlUtil.checkGlError("releaseGl done");

        mEglCore.makeNothingCurrent();
    }

    /**
     * Handles the surfaceChanged message.
     * <p>
     * We always receive surfaceChanged() after surfaceCreated(), but surfaceAvailable()
     * could also be called with a Surface created on a previous run.  So this may not
     * be called.
     */
    void surfaceChanged(int width, int height) {
        if (DBG) Log.d(TAG, "RenderThread surfaceChanged " + width + "x" + height);

        mWindowSurfaceWidth = width;
        mWindowSurfaceHeight = height;
        finishSurfaceSetup();
    }

    private void orientationChanged(int rotation) {
        if (DBG) Log.d(TAG, "orientationChanged() called with: " +  "rotation = [" + rotation + "]");
        mCameraRotation = rotation;
        updateGeometry();
    }

    private void freeze() {
        if (DBG) Log.d(TAG, "freeze()");
        mOnFreeze = true;
    }

    private void unfreeze() {
        if (DBG) Log.d(TAG, "unfreeze()");
        mOnFreeze = false;
    }

    /**
     * Handles the surfaceDestroyed message.
     */
    private void surfaceDestroyed() {

        // In practice this never appears to be called -- the activity is always paused
        // before the surface is destroyed.  In theory it could be called though.
        if (DBG) Log.d(TAG, "surfaceDestroyed()");
        releaseGl();
    }

    /**
     * Sets up anything that depends on the window size.
     * <p>
     * Open the camera (to set mCameraAspectRatio) before calling here.
     */
    private void finishSurfaceSetup() {
        int width = mWindowSurfaceWidth;
        int height = mWindowSurfaceHeight;
        if (DBG) Log.d(TAG, "finishSurfaceSetup size=" + width + "x" + height +
                " camera=" + mCameraPreviewWidth + "x" + mCameraPreviewHeight);

        // Use full window.
        GLES20.glViewport(0, 0, width, height);

        // Simple orthographic projection, with (0,0) in lower-left corner.
        Matrix.orthoM(mDisplayProjectionMatrix, 0, 0, width, 0, height, -1, 1);

        // Default position is center of screen.
        mPosX = width / 2.0f;
        mPosY = height / 2.0f;

        updateGeometry();

        // Ready to go, start the camera.
        if (DBG) Log.d(TAG, "starting camera preview");
        try {
            mCameraManager.startPreview(mCameraTexture);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Updates the geometry of mRect, based on the size of the window and the current
     * values set by the UI.
     */
    private void updateGeometry() {
        int viewWidth;
        int viewHeight;
        int newWidth;
        int newHeight;
        int previewWidth = mCameraPreviewWidth;
        int previewHeight = mCameraPreviewHeight;

        if (mCameraRotation % 180 == 0) {
            viewWidth = mWindowSurfaceWidth;
            viewHeight = mWindowSurfaceHeight;
        } else {
            viewWidth = mWindowSurfaceHeight;
            viewHeight = mWindowSurfaceWidth;
        }

        // Center crop
        if (previewWidth * viewHeight > previewHeight * viewWidth) {
            // Scale to height
            newWidth = (int)(previewWidth * viewHeight / (float)previewHeight + 0.5f);
            newHeight = viewHeight;
        } else {
            // Scale to width
            newWidth = viewWidth;
            newHeight = (int)(previewHeight * viewWidth / (float)previewWidth + 0.5f);
        }

        mRect.setScale(newWidth, newHeight);
        mRect.setPosition(mPosX, mPosY);
        mRect.setRotation((360 - mCameraRotation) % 360);
    }

    /**
     * Draws the scene and submits the buffer.
     */
    private void draw() {
        if (mOnFreeze) return;

        GlUtil.checkGlError("draw start");

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        mRect.draw(mTexProgram, mDisplayProjectionMatrix);
        mWindowSurface.swapBuffers();

        GlUtil.checkGlError("draw done");
    }

    /**
     * Handler for RenderThread.  Used for messages sent from the UI thread to the render thread.
     * <p>
     * The object is created on the render thread, and the various "send" methods are called
     * from the UI thread.
     */
    static class RenderHandler extends Handler {
        private static final int MSG_SURFACE_AVAILABLE = 0;
        private static final int MSG_SURFACE_CHANGED = 1;
        private static final int MSG_SURFACE_DESTROYED = 2;
        private static final int MSG_SHUTDOWN = 3;
        private static final int MSG_FRAME_AVAILABLE = 4;
        private static final int MSG_ORIENTATION_CHANGED = 5;
        private static final int MSG_REDRAW = 9;

        private static final int MSG_PAUSE_CAMERA = 10;
        private static final int MSG_RESUME_CAMERA = 11;
        private static final int MSG_PAUSE_PROCESS_FRAMES = 12;
        private static final int MSG_RESUME_PROCESS_FRAMES = 14;
        private static final int MSG_TOGGLE_FLASH = 15;
        private static final int MSG_REQUEST_FOCUS = 16;
        private static final int MSG_FREEZE = 17;
        private static final int MSG_UNFREEZE = 18;



        // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
        // but no real harm in it.
        private WeakReference<RenderThread> mWeakRenderThread;

        /**
         * Call from render thread.
         */
        public RenderHandler(RenderThread rt) {
            mWeakRenderThread = new WeakReference<RenderThread>(rt);
        }

        /**
         * Sends the "surface available" message.  If the surface was newly created (i.e.
         * this is called from surfaceCreated()), set newSurface to true.  If this is
         * being called during Activity startup for a previously-existing surface, set
         * newSurface to false.
         * <p>
         * The flag tells the caller whether or not it can expect a surfaceChanged() to
         * arrive very soon.
         * <p>
         * Call from UI thread.
         */
        public void sendSurfaceAvailable(SurfaceHolder holder, boolean newSurface) {
            sendMessage(obtainMessage(MSG_SURFACE_AVAILABLE,
                    newSurface ? 1 : 0, 0, holder));
        }

        /**
         * Sends the "surface changed" message, forwarding what we got from the SurfaceHolder.
         * <p>
         * Call from UI thread.
         */
        public void sendSurfaceChanged(@SuppressWarnings("unused") int format, int width,
                                       int height) {
            // ignore format
            sendMessage(obtainMessage(MSG_SURFACE_CHANGED, width, height));
        }

        /**
         * Sends the "shutdown" message, which tells the render thread to halt.
         * <p>
         * Call from UI thread.
         */
        public void sendSurfaceDestroyed() {
            sendMessage(obtainMessage(MSG_SURFACE_DESTROYED));
        }

        /**
         * Sends the "shutdown" message, which tells the render thread to halt.
         * <p>
         * Call from UI thread.
         */
        public void sendShutdown() {
            sendMessage(obtainMessage(MSG_SHUTDOWN));
        }

        /**
         * Sends the "frame available" message.
         * <p>
         * Call from UI thread.
         */
        public void sendFrameAvailable() {
            sendMessage(obtainMessage(MSG_FRAME_AVAILABLE));
        }

        public void sendOrientationChanged(int newRotation) {
            sendMessage(obtainMessage(MSG_ORIENTATION_CHANGED, newRotation, 0));
        }

        public void sendUnfreeze() {
            sendMessage(obtainMessage(MSG_UNFREEZE));
        }

        public void sendFreeze() {
            sendMessage(obtainMessage(MSG_FREEZE));
        }

        public void sendRequestFocus() {
            sendMessage(obtainMessage(MSG_REQUEST_FOCUS));
        }

        public void sendPauseCamera() {
            sendMessage(obtainMessage(MSG_PAUSE_CAMERA));
        }

        public void sendResumeCamera() {
            sendMessage(obtainMessage(MSG_RESUME_CAMERA));
        }

        public void sendPauseProcessFrames() {
            sendMessage(obtainMessage(MSG_PAUSE_PROCESS_FRAMES));
        }

        public void sendResumeProcessFrames() {
            sendMessage(obtainMessage(MSG_RESUME_PROCESS_FRAMES));
        }


        /**
         * Sends the "redraw" message.  Forces an immediate redraw.
         * <p>
         * Call from UI thread.
         */
        public void sendRedraw() {
            sendMessage(obtainMessage(MSG_REDRAW));
        }

        public void sendToggleFlash() {
            sendMessage(obtainMessage(MSG_TOGGLE_FLASH));
        }

        @Override  // runs on RenderThread
        public void handleMessage(Message msg) {
            int what = msg.what;
            //if (DBG) Log.d(TAG, "RenderHandler [" + this + "]: what=" + what);

            RenderThread renderThread = mWeakRenderThread.get();
            if (renderThread == null) {
                if (DBG) Log.w(TAG, "RenderHandler.handleMessage: weak ref is null");
                return;
            }

            switch (what) {
                case MSG_SURFACE_AVAILABLE:
                    renderThread.surfaceAvailable((SurfaceHolder) msg.obj, msg.arg1 != 0);
                    break;
                case MSG_SURFACE_CHANGED:
                    renderThread.surfaceChanged(msg.arg1, msg.arg2);
                    break;
                case MSG_SURFACE_DESTROYED:
                    renderThread.surfaceDestroyed();
                    break;
                case MSG_SHUTDOWN:
                    renderThread.shutdown();
                    break;
                case MSG_FRAME_AVAILABLE:
                    renderThread.frameAvailable();
                    break;
                case MSG_REDRAW:
                    renderThread.draw();
                    break;
                case MSG_ORIENTATION_CHANGED:
                    renderThread.orientationChanged(msg.arg1);
                    break;
                case MSG_FREEZE:
                    renderThread.freeze();
                    break;
                case MSG_UNFREEZE:
                    renderThread.unfreeze();
                    break;
                case MSG_TOGGLE_FLASH:
                    renderThread.mCameraManager.toggleFlash();
                    break;
                case MSG_REQUEST_FOCUS:
                    renderThread.mCameraManager.requestFocus();
                    break;
                case MSG_PAUSE_CAMERA:
                    renderThread.mCameraManager.pause();
                    break;
                case MSG_RESUME_CAMERA:
                    renderThread.mCameraManager.resume();
                    break;
                case MSG_PAUSE_PROCESS_FRAMES:
                    renderThread.mCameraManager.pauseProcessFrames();
                    break;
                case MSG_RESUME_PROCESS_FRAMES:
                    renderThread.mCameraManager.resumeProcessFrames();
                    break;
                default:
                    throw new RuntimeException("unknown message " + what);
            }
        }
    }
}