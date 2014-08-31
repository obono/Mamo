/*
 * Copyright (C) 2013, 2014 OBN-soft
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

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class CaptureCameraActivity extends CaptureActivity
        implements SurfaceHolder.Callback, PreviewCallback {

    private Camera      mCamera;
    private Camera.Size mCameraSize;
    private int         mCameraId;
    private SurfaceView mCamView;
    private int         mCamDeg;

    private boolean mFocusing;

    /*-----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.camera);
        super.onCreate(savedInstanceState);

        mCamView = (SurfaceView) findViewById(R.id.view_capimage);
        SurfaceHolder holder = mCamView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    protected void onResume() {
        if (mCamera != null) {
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.startPreview();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setCameraOrientation();
    }

    public void onShot(View v) {
        v.setEnabled(false);
        if (mCamera != null && !mFocusing) {
            setMessage(R.string.msg_focus);
            mFocusing = true;
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    setMessage(R.string.msg_capture);
                    mCamera.addCallbackBuffer(
                            new byte[mCameraSize.width * mCameraSize.height * 3 / 2]);
                    camera.autoFocus(null);
                    mFocusing = false;
                }
            });
        }
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            int size = getFrameSize();
            CameraInfo info = new CameraInfo();
            mCameraId = 0;
            for (int i = 0, c = Camera.getNumberOfCameras(); i < c; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                    mCameraId = i;
                    break;
                }
            }
            mCamera = Camera.open(mCameraId);
            if (mCamera == null) {
                Toast.makeText(this, R.string.msg_notsupported, Toast.LENGTH_LONG).show();
                setCanceledResult();
                return;
            }
            mCamera.setPreviewDisplay(holder);
            Camera.Parameters cp = mCamera.getParameters();
            List<Camera.Size> sizeList = cp.getSupportedPreviewSizes();
            mCameraSize = null;
            for (Camera.Size s : sizeList) {
                if (s.width >= size && s.height >= size && (mCameraSize == null ||
                        s.width * s.height < mCameraSize.width * mCameraSize.height)) {
                    mCameraSize = s;
                }
            }
            if (mCameraSize == null) {
                mCameraSize = sizeList.get(0);
            }
            cp.setPreviewSize(mCameraSize.width, mCameraSize.height);
            mCamera.setParameters(cp);
            try {
                cp.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                mCamera.setParameters(cp);
            } catch (RuntimeException e2) {
                e2.printStackTrace();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        setCameraOrientation();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //setCameraOrientation();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        int dw = mCameraSize.width;
        int dh = mCameraSize.height;
        int size = getFrameSize();
        int gw = (dw - size) / 2;
        int gh = (dh - size) / 2;
        YuvImage yuvimage;
        if (data != null) {
            yuvimage = new YuvImage(data, ImageFormat.NV21, dw, dh, null);
            String fname = TargetUtils.getTargetFileName();
            TargetUtils.pileHistoryFile(this, fname);
            try {
                final int compression = 80;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                yuvimage.compressToJpeg(new Rect(gw, gh, dw - gw, dh - gh), compression, baos);
                Bitmap bitmap = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size());
                Matrix matrix = new Matrix();
                matrix.postRotate(mCamDeg);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, size, size, matrix, false);
                FileOutputStream out = openFileOutput(fname, MODE_PRIVATE);
                bitmap.compress(Bitmap.CompressFormat.JPEG, compression, out);
                out.close();
                setSuccessResult();
            } catch (IOException e) {
                e.printStackTrace();
                setCanceledResult();
            }
        } else {
            setCanceledResult();
        }
    }

    /*-----------------------------------------------------------------------*/

    private void setCameraOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int dispDeg = 0;
        switch (rotation) {
            case Surface.ROTATION_0:   dispDeg = 0;   break;
            case Surface.ROTATION_90:  dispDeg = 90;  break;
            case Surface.ROTATION_180: dispDeg = 180; break;
            case Surface.ROTATION_270: dispDeg = 270; break;
        }
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
            mCamDeg = (360 + info.orientation - dispDeg) % 360;
        } else {
            mCamDeg = (720 - info.orientation - dispDeg) % 360;
        }

        int w, h;
        if (mCamDeg % 180 == 90) {
            w = mCameraSize.height;
            h = mCameraSize.width;
        } else {
            w = mCameraSize.width;
            h = mCameraSize.height;
        }
        mCamera.stopPreview();
        if (w != mCamView.getWidth() || h != mCamView.getHeight()) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(w, h);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
            mCamView.setLayoutParams(lp);
        }
        mCamera.setDisplayOrientation(mCamDeg);
        mCamera.setPreviewCallbackWithBuffer(this);
        try {
            mCamera.startPreview();
        } catch (RuntimeException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.msg_notsupported, Toast.LENGTH_LONG).show();
            setCanceledResult();
        }
    }

}
