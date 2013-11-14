/*
 * Copyright (C) 2013 OBN-soft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.obnsoft.mamo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;

public class MyRenderer implements Renderer {

    public static final String FNAME_TARGET = "target.img";

    private static final int BYTES_PAR_FLOAT = 4;
    private static final float TARGET_SIZE = ElementsManager.HIT_SIZE / 2f;
    private static final float PIECE_SIZE = TARGET_SIZE / 4f;
    private static final float[] VERTICES = {
        -TARGET_SIZE,   TARGET_SIZE,
        TARGET_SIZE,    TARGET_SIZE,
        TARGET_SIZE,    -TARGET_SIZE,
        -TARGET_SIZE,   -TARGET_SIZE,
        -PIECE_SIZE,    PIECE_SIZE,
        PIECE_SIZE,     PIECE_SIZE,
        PIECE_SIZE,     -PIECE_SIZE,
        -PIECE_SIZE,    -PIECE_SIZE,
    };
    private static final float[] TEXCOORDS = {
        0f, 0f,     1f, 0f,     1f, 1f,     0f, 1f,
    };

    private final Context mContext;
    private final ElementsManager mManager;

    private boolean mIsLoadedTexture = false;

    /*-----------------------------------------------------------------------*/

    public MyRenderer(Context context, ElementsManager manager) {
        mContext = context;
        mManager = manager;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initializeBuffers((GL11) gl);
        gl.glEnable(GL10.GL_TEXTURE_2D);
        int[] buffers = new int[1];
        gl.glGenTextures(1, buffers, 0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, buffers[0]);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        loadTexture();

        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL10.GL_BLEND);

        gl.glEnable(GL10.GL_LIGHTING);
        gl.glEnable(GL10.GL_LIGHT0);
        gl.glClearColor(0f, 0.2f, 0.4f, 1f);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        float aspect = (float) width / (float) height;
        float rangeX = 0.5f;
        float rangeY = 0.5f;
        if (aspect < 1f) {
            rangeY /= aspect;
        } else {
            rangeX *= aspect;
        }
        gl.glOrthof(-rangeX, rangeX, -rangeY, rangeY, 0.1f, 100f);
        mManager.setFieldRange(rangeX, rangeY);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        loadTexture();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mManager.forwardElements();
        float flash = mManager.getFlashLevel();
        if (flash >= 0f) {
            gl.glClearColor(flash, flash + 0.2f, flash + 0.4f, 1f);
        }
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
        GL11 gl11 = (GL11) gl;
        ArrayList<Element> ary = mManager.getArrayList();
        synchronized(ary) {
            for (Element e : ary) {
                if (e.type != -1) {
                    gl.glPushMatrix();
                    gl.glTranslatef(e.x, e.y, -1f);
                    gl.glRotatef(e.r, 0, 0, 1);
                    gl11.glVertexPointer(2, GL10.GL_FLOAT, 0,
                            (e.type == 0) ? 0 : 8 * BYTES_PAR_FLOAT);
                    gl11.glTexCoordPointer(2, GL10.GL_FLOAT, 0,
                            (e.type * 8 + 16) * BYTES_PAR_FLOAT);
                    gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4);
                    gl.glPopMatrix();
                }
            }
        }
    }

    public void setToReloadTexture() {
        mIsLoadedTexture = false;
    }

    /*-----------------------------------------------------------------------*/

    private void initializeBuffers(GL11 gl11) {
        FloatBuffer vertexBuf = newFloatBuffer(19 * 4 * 2);
        vertexBuf.position(0);
        vertexBuf.put(VERTICES);
        vertexBuf.put(TEXCOORDS);
        for (int y1 = 0; y1 < 4; y1++) {
            int y2 = y1 + 1;
            for (int x1 = 0; x1 < 4; x1++) {
                int x2 = x1 + 1;
                vertexBuf.put(x1 / 4f);
                vertexBuf.put(y1 / 4f);
                vertexBuf.put(x2 / 4f);
                vertexBuf.put(y1 / 4f);
                vertexBuf.put(x2 / 4f);
                vertexBuf.put(y2 / 4f);
                vertexBuf.put(x1 / 4f);
                vertexBuf.put(y2 / 4f);
            }
        }

        int[] buffers = new int[1];
        gl11.glGenBuffers(1, buffers, 0);
        gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, buffers[0]);
        vertexBuf.position(0);
        gl11.glBufferData(GL11.GL_ARRAY_BUFFER, vertexBuf.capacity() * BYTES_PAR_FLOAT,
                vertexBuf, GL11.GL_STATIC_DRAW);
    }

    private FloatBuffer newFloatBuffer(int size) {
        ByteBuffer bb = ByteBuffer.allocateDirect(size * BYTES_PAR_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        return bb.asFloatBuffer();
    }

    /*private FloatBuffer getFloatBufferFromArray(float[] array) {
        FloatBuffer ret;
        ByteBuffer bb = ByteBuffer.allocateDirect(array.length * BYTES_PAR_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        ret = bb.asFloatBuffer();
        ret.put(array);
        ret.position(0);
        return ret;
    }*/

    private void loadTexture() {
        if (!mIsLoadedTexture) {
            Bitmap bitmap;
            try {
                InputStream in = mContext.openFileInput(FNAME_TARGET);
                bitmap = BitmapFactory.decodeStream(in);
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
                bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.obono256);
            }
            Bitmap bitmap2 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            bitmap.recycle();
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int cx = w / 2, cy = h / 2, size = Math.min(cx, cy);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (Math.hypot(x - cx, y - cy) > size) {
                        bitmap2.setPixel(x, y, Color.TRANSPARENT);
                    }
                }
            }
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap2, 0);
            bitmap2.recycle();
            mIsLoadedTexture = true;
        }
    }

}
