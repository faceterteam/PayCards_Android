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

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.support.annotation.RestrictTo;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * Recordable EGL window surface.
 * <p>
 * It's good practice to explicitly release() the surface, preferably from a "finally" block.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class WindowSurface {
    protected static final String TAG = GlUtil.TAG;
    private static final boolean DBG = GlUtil.DBG;
    // EglCore object we're associated with.  It may be associated with multiple surfaces.
    protected EglCore mEglCore;
    private Surface mSurface;
    private boolean mReleaseSurface;
    private EGLSurface mEGLSurface = null;
    private int mWidth = -1;
    private int mHeight = -1;

    /**
     * Associates an EGL surface with the native window surface.
     * <p>
     * Set releaseSurface to true if you want the Surface to be released when release() is
     * called.  This is convenient, but can interfere with framework classes that expect to
     * manage the Surface themselves (e.g. if you release a SurfaceView's Surface, the
     * surfaceDestroyed() callback won't fire).
     */
    public WindowSurface(EglCore eglCore, SurfaceHolder surfaceHolder, boolean releaseSurface) {
        mEglCore = eglCore;
        createWindowSurface(surfaceHolder);
        mSurface = surfaceHolder.getSurface();
        mReleaseSurface = releaseSurface;
    }

    /**
     * Releases any resources associated with the EGL surface (and, if configured to do so,
     * with the Surface as well).
     * <p>
     * Does not require that the surface's EGL context be current.
     */
    public void release() {
        releaseEglSurface();
        if (mSurface != null) {
            if (mReleaseSurface) {
                mSurface.release();
            }
            mSurface = null;
        }
    }

    /**
     * Creates a window surface.
     * <p>
     * @param surface May be a Surface or SurfaceTexture.
     */
    public void createWindowSurface(Object surface) {
        if (mEGLSurface != null) {
            throw new IllegalStateException("surface already created");
        }
        mEGLSurface = mEglCore.createWindowSurface(surface);

        // Don't cache width/height here, because the size of the underlying surface can change
        // out from under us (see e.g. HardwareScalerActivity).
        //mWidth = mEglCore.querySurface(mEGLSurface, EGL14.EGL_WIDTH);
        //mHeight = mEglCore.querySurface(mEGLSurface, EGL14.EGL_HEIGHT);
    }

    /**
     * Returns the surface's width, in pixels.
     * <p>
     * If this is called on a window surface, and the underlying surface is in the process
     * of changing size, we may not see the new size right away (e.g. in the "surfaceChanged"
     * callback).  The size should match after the next buffer swap.
     */
    public int getWidth() {
        if (mWidth < 0) {
            return mEglCore.querySurface(mEGLSurface, EGL10.EGL_WIDTH);
        } else {
            return mWidth;
        }
    }

    /**
     * Returns the surface's height, in pixels.
     */
    public int getHeight() {
        if (mHeight < 0) {
            return mEglCore.querySurface(mEGLSurface, EGL10.EGL_HEIGHT);
        } else {
            return mHeight;
        }
    }

    /**
     * Release the EGL surface.
     */
    public void releaseEglSurface() {
        mEglCore.releaseSurface(mEGLSurface);
        mEGLSurface = null;
        mWidth = mHeight = -1;
    }

    /**
     * Makes our EGL context and surface current.
     */
    public void makeCurrent() {
        mEglCore.makeCurrent(mEGLSurface);
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     *
     * @return false on failure
     */
    public boolean swapBuffers() {
        boolean result = mEglCore.swapBuffers(mEGLSurface);
        if (!result) {
            Log.d(TAG, "WARNING: swapBuffers() failed");
        }
        return result;
    }

    /**
     * Saves the EGL surface to a file.
     * <p>
     * Expects that this object's EGL surface is current.
     */
    public void saveFrame(File file) throws IOException {
        if (!mEglCore.isCurrent(mEGLSurface)) {
            throw new RuntimeException("Expected EGL context/surface is not current");
        }

        // glReadPixels fills in a "direct" ByteBuffer with what is essentially big-endian RGBA
        // data (i.e. a byte of red, followed by a byte of green...).  While the Bitmap
        // constructor that takes an int[] wants little-endian ARGB (blue/red swapped), the
        // Bitmap "copy pixels" method wants the same format GL provides.
        //
        // Ideally we'd have some way to re-use the ByteBuffer, especially if we're calling
        // here often.
        //
        // Making this even more interesting is the upside-down nature of GL, which means
        // our output will look upside down relative to what appears on screen if the
        // typical GL conventions are used.

        String filename = file.toString();

        int width = getWidth();
        int height = getHeight();
        ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        GLES20.glReadPixels(0, 0, width, height,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
        GlUtil.checkGlError("glReadPixels");
        buf.rewind();

        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(filename));
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(buf);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
            bmp.recycle();
        } finally {
            if (bos != null) bos.close();
        }
        if (DBG) Log.d(TAG, "Saved " + width + "x" + height + " frame as '" + filename + "'");
    }
}
