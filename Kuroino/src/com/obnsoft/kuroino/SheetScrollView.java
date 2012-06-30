/*
 * Copyright (C) 2012 OBN-soft
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

package com.obnsoft.kuroino;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.obnsoft.view.FreeScrollView;

public class SheetScrollView extends FreeScrollView {

    private SheetData mData = null;
    private SheetView mChild = null;
    private int mFocusRow = -1;
    private int mFocusCol = -1;

    /*----------------------------------------------------------------------*/

    class SheetView extends View {

        private View mParent = null;
        private boolean mIsFocus;
        private Paint mPaintGrid = new Paint();
        private Paint mPaintText = new Paint(Paint.ANTI_ALIAS_FLAG);

        public SheetView(View parent) {
            super(parent.getContext());
            mParent = parent;
            mPaintGrid.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaintText.setColor(Color.WHITE);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            resize();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (mData != null) {
                    int row = (int) event.getY() / mData.cellSize;
                    int col = (int) event.getX() / mData.cellSize;
                    if (row >= 0 && row < mData.entries.size() &&
                            col >= 0 && col < mData.dates.size()) {
                        mIsFocus = true;
                        mFocusRow = row;
                        mFocusCol = col;
                        ((MainActivity) getContext()).handleFocus(mParent, mFocusRow, mFocusCol);
                        postInvalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsFocus) {
                    ((MainActivity) getContext()).handleClick(mParent, mFocusRow, mFocusCol);
                    postInvalidate();
                    mIsFocus = false;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsFocus) {
                    mFocusCol = mFocusRow = -1;
                    ((MainActivity) getContext()).handleFocus(mParent, -1, -1);
                    postInvalidate();
                    mIsFocus = false;
                }
                break;
            }
            return (mIsFocus) ? true : super.onTouchEvent(event);
        }

        @Override
        public void onDraw(Canvas c) {
            if (mData == null) {
                return;
            }

            int cellSize = mData.cellSize;
            int rows = mData.entries.size();
            int cols = mData.dates.size();

            int scrollX = mParent.getScrollX();
            int scrollY = mParent.getScrollY();
            int scrollWidth = mParent.getWidth();
            int scrollHeight = mParent.getHeight();

            int startRow = Math.max(scrollY / cellSize, 0);
            int endRow = Math.min((scrollY + scrollHeight - 1) / cellSize, rows - 1);
            int startCol = Math.max(scrollX / cellSize, 0);
            int endCol = Math.min((scrollX + scrollWidth - 1) / cellSize, cols - 1);

            /*  Symbol  */
            mPaintText.setTextSize(mData.cellSize * 0.75f);
            FontMetrics fm = mPaintText.getFontMetrics();
            float strHeight = fm.ascent + fm.descent;
            for (int row = startRow; row <= endRow; row++) {
                ArrayList<String> attends = mData.entries.get(row).attends;
                for (int col = startCol; col <= endCol; col++) {
                    String str = attends.get(col);
                    if (str != null) {
                        float strWidth = mPaintText.measureText(str);
                        c.drawText(str, col * cellSize + (cellSize - strWidth) / 2f,
                                row * cellSize + (cellSize - strHeight) / 2f, mPaintText);
                    }
                }
            }

            /*  Grid  */
            mPaintGrid.setColor(Color.WHITE);
            for (int row = startRow; row <= endRow + 1; row++) {
                c.drawLine(scrollX, row * cellSize,
                        scrollX + scrollWidth, row * cellSize, mPaintGrid);
            }
            for (int col = startCol; col <= endCol + 1; col++) {
                c.drawLine(col * cellSize, scrollY,
                        col * cellSize, scrollY + scrollHeight, mPaintGrid);
            }

            /*  Focus  */
            mPaintGrid.setColor(Color.argb(31, 255, 255, 0));
            if (mFocusRow >= 0) {
                c.drawRect(scrollX, mFocusRow * cellSize,
                        scrollX + scrollWidth, (mFocusRow + 1) * cellSize, mPaintGrid);
            }
            if (mFocusCol >= 0) {
                c.drawRect(mFocusCol * cellSize, scrollY,
                        (mFocusCol + 1) * cellSize, scrollY + scrollHeight, mPaintGrid);
            }
            if (mIsFocus && mFocusRow >= 0 && mFocusCol >= 0) {
                mPaintGrid.setColor(Color.argb(63, 255, 255, 0));
                c.drawRect(mFocusCol * cellSize, mFocusRow * cellSize,
                        (mFocusCol + 1) * cellSize, (mFocusRow + 1) * cellSize, mPaintGrid);
            }
        }

        private void resize() {
            int width = 1;
            int height = 1;
            if (mData != null) {
                width = mData.dates.size() * mData.cellSize + 1;
                height = mData.entries.size() * mData.cellSize + 1;
            }
            setMeasuredDimension(width, height);
            layout(0, 0, width, height);
        }

    }

    /*----------------------------------------------------------------------*/

    public SheetScrollView(Context context) {
        super(context);
        initialize();
    }
    public SheetScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }
    public SheetScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    public void setSheetData(SheetData data) {
        mData = data;
    }

    public void refreshView() {
        mChild.resize();
        postInvalidate();
    }

    public void setFocus(int row, int col) {
        mFocusRow = row;
        mFocusCol = col;
        mChild.postInvalidate();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        ((MainActivity) getContext()).handleScroll(this, l, t);
    }

    private void initialize() {
        mChild = new SheetView(this);
        addView(mChild);
    }
}
