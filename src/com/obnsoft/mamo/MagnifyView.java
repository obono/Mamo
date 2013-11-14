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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;

public class MagnifyView extends View implements OnScaleGestureListener {

    private boolean mIsMoving;
    private boolean mIsScaling;
    private float   mScale;
    private float   mFocusX;
    private float   mFocusY;

    private Bitmap  mBitmap;
    private Rect    mWorkRect = new Rect();
    private Rect    mSrcRect = new Rect();
    private RectF   mDrawRect = new RectF();

    private ScaleGestureDetector mGestureDetector;

    /*-----------------------------------------------------------------------*/

    public MagnifyView(Context context) {
        this(context, null);
    }

    public MagnifyView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MagnifyView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mGestureDetector = new ScaleGestureDetector(context, this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmap == null) {
            return;
        }

        canvas.getClipBounds(mWorkRect);
        if (mWorkRect.isEmpty()) {
            mWorkRect.set(0, 0, getWidth(), getHeight());
        }
        canvas.drawBitmap(mBitmap, mSrcRect, mDrawRect, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calcCoords();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        mGestureDetector.onTouchEvent(event);
        boolean ret = mIsScaling;
        if (!mIsScaling) {
            switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (mIsMoving) {
                    mDrawRect.offset(x - mFocusX, y - mFocusY);
                    mFocusX = x;
                    mFocusY = y;
                    adjustDrawRect();
                    invalidate();
                    ret = true;
                    break;
                }
                // go to following code.
            case MotionEvent.ACTION_DOWN:
                mIsMoving = true;
                mFocusX = x;
                mFocusY = y;
                ret = true;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mIsMoving) {
                    mIsMoving = false;
                    ret = true;
                }
                break;
            }
        }
        return ret;
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float factor = detector.getScaleFactor();
        if (factor != 1f) {
            float x = detector.getFocusX();
            float y = detector.getFocusY();
            mDrawRect.offset(x - mFocusX, y - mFocusY);
            mFocusX = x;
            mFocusY = y;
            float dx = x - (x - mDrawRect.left) * factor;
            float dy = y - (y - mDrawRect.top) * factor;
            mScale *= factor;
            mDrawRect.set(dx, dy,
                    dx + mSrcRect.width() * mScale, dy + mSrcRect.height() * mScale);
            adjustDrawRect();
            invalidate();
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mIsMoving = false;
        if (mBitmap != null) {
            mFocusX = detector.getFocusX();
            mFocusY = detector.getFocusY();
            mIsScaling = true;
            return true;
        }
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mIsScaling = false;
    }

    /*-----------------------------------------------------------------------*/

    public void setBitmap(Bitmap bmp) {
        mBitmap = bmp;
        if (bmp == null) {
            mSrcRect.setEmpty();
        } else {
            mSrcRect.set(0, 0, bmp.getWidth(), bmp.getHeight());
        }
        calcCoords();
        invalidate();
    }

    public RectF getBitmapDrawRect(RectF outRect) {
        if (outRect != null) {
            outRect.set(mDrawRect);
        }
        return outRect;
    }

    /*-----------------------------------------------------------------------*/

    private void calcCoords() {
        if (mBitmap == null) {
            mDrawRect.set(0, 0, 0, 0);
            return;
        }
        float sw = mSrcRect.width();
        float sh = mSrcRect.height();
        float dw = getWidth();
        float dh = getHeight();
        if (sw == 0 || sh == 0 || dw == 0 || dh == 0) {
            mDrawRect.set(0, 0, 0, 0);
            return;
        }
        mScale = Math.min((dw - 2f) / sw, (dh - 2f) / sh);
        float dx = (dw - sw * mScale) / 2f;
        float dy = (dh - sh * mScale) / 2f;
        mDrawRect.set(dx, dy, dx + sw * mScale, dy + sh * mScale);
    }

    private void adjustDrawRect() {
        int dw = getWidth();
        int dh = getHeight();
        int mw = dw / 2;
        int mh = dh / 2;

        if (dw - mDrawRect.width() > mw * 2) {
            mDrawRect.offset((dw - mDrawRect.width()) / 2 - mDrawRect.left, 0);
        } else if (mDrawRect.left > mw){
            mDrawRect.offset(mw - mDrawRect.left, 0);
        } else if (dw - mDrawRect.right > mw){
            mDrawRect.offset(dw - mDrawRect.right - mw, 0);
        }

        if (dh - mDrawRect.height() > mh * 2) {
            mDrawRect.offset(0, (dh - mDrawRect.height()) / 2 - mDrawRect.top);
        } else if (mDrawRect.top > mh){
            mDrawRect.offset(0, mh - mDrawRect.top);
        } else if (dh - mDrawRect.bottom > mh){
            mDrawRect.offset(0, dh - mDrawRect.bottom - mh);
        }
    }

}
