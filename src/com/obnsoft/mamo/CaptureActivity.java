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

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class CaptureActivity extends Activity {

    private View            mCapFrame;
    private TextView        mLabelMsg;
    private ShapeDrawable   mCamFrameDrawable;

    /*-----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCapFrame = (View) findViewById(R.id.view_capframe);
        mCamFrameDrawable = new ShapeDrawable(new OvalShape());
        Paint paint = mCamFrameDrawable.getPaint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Color.RED);
        mCapFrame.setBackgroundDrawable(mCamFrameDrawable);
        mLabelMsg = (TextView) findViewById(R.id.text_capmsg);
    }

    protected int getFrameSize() {
        return mCapFrame.getWidth();
    }

    protected void setMessage(int msgId) {
        mLabelMsg.setText(msgId);
    }

    protected void setSuccessResult() {
        setResult(RESULT_OK);
        finish();
    }

    protected void setCanceledResult() {
        setResult(RESULT_CANCELED);
        finish();
    }

}
