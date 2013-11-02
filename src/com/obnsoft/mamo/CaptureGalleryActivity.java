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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class CaptureGalleryActivity extends CaptureActivity {

    private static final int REQUEST_ID_CHOOSE_FILE = 1;

    private boolean mIsOnResult;
    private Uri mUri;
    private Bitmap mBitmap;

    private MagnifyView mImgView;

    /*-----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.gallery);
        super.onCreate(savedInstanceState);

        setMessage(R.string.msg_pinch);
        mImgView = (MagnifyView) findViewById(R.id.view_capimage);
    }

    @Override
    protected void onResume() {
        if (mIsOnResult) {
            mIsOnResult = false;
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_ID_CHOOSE_FILE);
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (mBitmap != null) {
            mImgView.setBitmap(null);
            mBitmap.recycle();
            mBitmap = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
        case REQUEST_ID_CHOOSE_FILE:
            if (resultCode == RESULT_OK) {
                mUri = intent.getData();
                if (mUri != null) {
                    if (mBitmap != null) {
                        mBitmap.recycle();
                    }
                    try {
                        InputStream in = getContentResolver().openInputStream(mUri);
                        mBitmap = BitmapFactory.decodeStream(in);
                        in.close();
                        mImgView.setBitmap(mBitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (mBitmap == null) {
                setCanceledResult();
            }
            break;
        }
        mIsOnResult = true;  // This function is called before onResume()
    }

    public void onShot(View v) {
        int size = getFrameSize();
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(Color.TRANSPARENT);
        Canvas canvas = new Canvas(bmp);
        RectF destRect = mImgView.getBitmapDrawRect(new RectF());
        destRect.offset((size - mImgView.getWidth()) / 2f, (size - mImgView.getHeight()) / 2f);
        canvas.drawBitmap(mBitmap,
                new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight()), destRect, null);
        try {
            FileOutputStream out = openFileOutput(MyRenderer.FNAME_TARGET, MODE_PRIVATE);
            bmp.compress(CompressFormat.PNG, 80, out);
            out.close();
            setSuccessResult();
        } catch (IOException e) {
            e.printStackTrace();
            setCanceledResult();
        }
        bmp.recycle();
    }

    /*-----------------------------------------------------------------------*/

}
