/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.pay.paycardsrecognizer.sdk.camera.gles;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.support.annotation.RestrictTo;
import android.util.Log;
import android.view.SurfaceHolder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class EglCore {
    private static final boolean DBG = GlUtil.DBG;
    private static final String TAG = GlUtil.TAG;

    /**
     * Constructor flag: surface must be recordable.  This discourages EGL from using a
     * pixel format that cannot be converted efficiently to something usable by the video
     * encoder.
     */
    public static final int FLAG_RECORDABLE = 0x01;

    // Android-specific extension.
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    private EGL10 mEgl;
    private EGLDisplay mEGLDisplay = EGL10.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL10.EGL_NO_CONTEXT;
    private EGLConfig mEGLConfig = null;
    private int mGlVersion = -1;

    /**
     * Prepares EGL display and context.
     * <p>
     * Equivalent to EglCore(null, 0).
     */
    public EglCore() {
        this(null, 0);
    }

    /**
     * Prepares EGL display and context.
     * <p>
     * @param sharedContext The context to share, or null if sharing is not desired.
     * @param flags Configuration bit flags, e.g. FLAG_RECORDABLE.
     */
    public EglCore(EGLContext sharedContext, int flags) throws RuntimeException {
        if (mEGLDisplay != EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("EGL already set up");
        }

        if (sharedContext == null) {
            sharedContext = EGL10.EGL_NO_CONTEXT;
        }

        mEgl = (EGL10) EGLContext.getEGL();

        mEGLDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!mEgl.eglInitialize(mEGLDisplay, version)) {
            mEGLDisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }

        EGLConfig config = getConfig(flags);
        if (config == null) {
            throw new RuntimeException("Unable to find a suitable EGLConfig");
        }
        int[] attrib2_list = {
                EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
        };
        EGLContext context = mEgl.eglCreateContext(mEGLDisplay, config, EGL10.EGL_NO_CONTEXT, attrib2_list);
        checkEglError("eglCreateContext");
        mEGLConfig = config;
        mEGLContext = context;
        mGlVersion = 2;

        // Confirm with query.
        int[] values = new int[1];
        mEgl.eglQueryContext(mEGLDisplay, mEGLContext, EGL_CONTEXT_CLIENT_VERSION,
                values);
        if (DBG) Log.d(TAG, "EGLContext created, client version " + values[0]);
    }

    /**
     * Finds a suitable EGLConfig.
     *
     * @param flags Bit flags from constructor.
     */
    private EGLConfig getConfig(int flags) {
        int renderableType = /* EGL14.EGL_OPENGL_ES2_BIT */ 4;

        // The actual surface is generally RGBA or RGBX, so situationally omitting alpha
        // doesn't really help.  It can also lead to a huge performance hit on glReadPixels()
        // when reading into a GL_RGBA buffer.
        int[] attribList = {
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                //EGL14.EGL_DEPTH_SIZE, 16,
                //EGL14.EGL_STENCIL_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, renderableType,
                EGL10.EGL_NONE, 0,      // placeholder for recordable [@-3]
                EGL10.EGL_NONE
        };
        if ((flags & FLAG_RECORDABLE) != 0) {
            attribList[attribList.length - 3] = EGL_RECORDABLE_ANDROID;
            attribList[attribList.length - 2] = 1;
        }
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!mEgl.eglChooseConfig(mEGLDisplay, attribList, configs, configs.length, numConfigs)) {
            if (DBG) Log.w(TAG, "unable to find RGB8888 / " + 2 + " EGLConfig");
            return null;
        }
        return configs[0];
    }

    /**
     * Discards all resources held by this class, notably the EGL context.  This must be
     * called from the thread where the context was created.
     * <p>
     * On completion, no context will be current.
     */
    public void release() {
        if (mEGLDisplay != EGL10.EGL_NO_DISPLAY) {
            // Android is unusual in that it uses a reference-counted EGLDisplay.  So for
            // every eglInitialize() we need an eglTerminate().
            mEgl.eglMakeCurrent(mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT);
            mEgl.eglDestroyContext(mEGLDisplay, mEGLContext);
            // TODO mEgl.eglReleaseThread();
            mEgl.eglTerminate(mEGLDisplay);
        }

        mEGLDisplay = EGL10.EGL_NO_DISPLAY;
        mEGLContext = EGL10.EGL_NO_CONTEXT;
        mEGLConfig = null;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mEGLDisplay != EGL10.EGL_NO_DISPLAY) {
                // We're limited here -- finalizers don't run on the thread that holds
                // the EGL state, so if a surface or context is still current on another
                // thread we can't fully release it here.  Exceptions thrown from here
                // are quietly discarded.  Complain in the log file.
                if (DBG) Log.w(TAG, "WARNING: EglCore was not explicitly released -- state may be leaked");
                release();
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Destroys the specified surface.  Note the EGLSurface won't actually be destroyed if it's
     * still current in a context.
     */
    public void releaseSurface(EGLSurface eglSurface) {
        mEgl.eglDestroySurface(mEGLDisplay, eglSurface);
    }

    /**
     * Creates an EGL surface associated with a Surface.
     * <p>
     * If this is destined for MediaCodec, the EGLConfig should have the "recordable" attribute.
     */
    public EGLSurface createWindowSurface(Object surface) {
        if (!(surface instanceof SurfaceHolder) && !(surface instanceof SurfaceTexture)) {
            throw new RuntimeException("invalid surface: " + surface);
        }

        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttribs = {
                EGL10.EGL_NONE
        };
        EGLSurface eglSurface = mEgl.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, surface,
                surfaceAttribs);
        checkEglError("eglCreateWindowSurface");
        if (eglSurface == null) {
            throw new RuntimeException("surface was null");
        }
        return eglSurface;
    }

    /**
     * Creates an EGL surface associated with an offscreen buffer.
     */
    public EGLSurface createOffscreenSurface(int width, int height) {
        int[] surfaceAttribs = {
                EGL10.EGL_WIDTH, width,
                EGL10.EGL_HEIGHT, height,
                EGL10.EGL_NONE
        };
        EGLSurface eglSurface = mEgl.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig,
                surfaceAttribs);
        checkEglError("eglCreatePbufferSurface");
        if (eglSurface == null) {
            throw new RuntimeException("surface was null");
        }
        return eglSurface;
    }

    /**
     * Makes our EGL context current, using the supplied surface for both "draw" and "read".
     */
    public void makeCurrent(EGLSurface eglSurface) {
        if (mEGLDisplay == EGL10.EGL_NO_DISPLAY) {
            // called makeCurrent() before create?
            if (DBG) Log.d(TAG, "NOTE: makeCurrent w/o display");
        }
        if (!mEgl.eglMakeCurrent(mEGLDisplay, eglSurface, eglSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    /**
     * Makes our EGL context current, using the supplied "draw" and "read" surfaces.
     */
    public void makeCurrent(EGLSurface drawSurface, EGLSurface readSurface) {
        if (mEGLDisplay == EGL10.EGL_NO_DISPLAY) {
            // called makeCurrent() before create?
            if (DBG) Log.d(TAG, "NOTE: makeCurrent w/o display");
        }
        if (!mEgl.eglMakeCurrent(mEGLDisplay, drawSurface, readSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent(draw,read) failed");
        }
    }

    /**
     * Makes no context current.
     */
    public void makeNothingCurrent() {
        if (!mEgl.eglMakeCurrent(mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_CONTEXT)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     *
     * @return false on failure
     */
    public boolean swapBuffers(EGLSurface eglSurface) {
        return mEgl.eglSwapBuffers(mEGLDisplay, eglSurface);
    }


    /**
     * Returns true if our context and the specified surface are current.
     */
    public boolean isCurrent(EGLSurface eglSurface) {
        return mEGLContext.equals(mEgl.eglGetCurrentContext()) &&
                eglSurface.equals(mEgl.eglGetCurrentSurface(EGL10.EGL_DRAW));
    }

    /**
     * Performs a simple surface query.
     */
    public int querySurface(EGLSurface eglSurface, int what) {
        int[] value = new int[1];
        mEgl.eglQuerySurface(mEGLDisplay, eglSurface, what, value);
        return value[0];
    }

    /**
     * Queries a string value.
     */
    public String queryString(int what) {
        return mEgl.eglQueryString(mEGLDisplay, what);
    }

    /**
     * Returns the GLES version this context is configured for (currently 2 or 3).
     */
    public int getGlVersion() {
        return mGlVersion;
    }

    /**
     * Checks for EGL errors.  Throws an exception if an error has been raised.
     */
    private void checkEglError(String msg) {
        int error;
        if ((error = mEgl.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }
}
