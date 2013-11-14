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

import java.util.ArrayList;

public class ElementsManager {

    public static final float HIT_SIZE = 1f / 8f;

    private ArrayList<Element> mElementArray = new ArrayList<Element>();
    private ArrayList<Element> mWorkArray = new ArrayList<Element>();
    private int     mFrames = 0;
    private float   mRangeX = 0.5f;
    private float   mRangeY = 0.5f;
    private float   mFlashLevel = 0f;
    private int     mInterval = 60;
    private boolean mSpeedTrick = false;
    private boolean mRotationTrick = false;
    private boolean mAngleTrick = false;

    /*-----------------------------------------------------------------------*/

    public void initialize() {
        synchronized (mElementArray) {
            mElementArray.clear();
            mWorkArray.clear();
            mFrames = 0;
        }
    }

    public void setInterval(int interval) {
        mInterval = interval;
    }

    public void setTricks(boolean speed, boolean rotation, boolean angle) {
        mSpeedTrick = speed;
        mRotationTrick = rotation;
        mAngleTrick = angle;
    }

    public void setFieldRange(float rangeX, float rangeY) {
        mRangeX = rangeX;
        mRangeY = rangeY;
    }

    public void forwardElements() {
        synchronized (mElementArray) {
            if (++mFrames % mInterval == 0) {
                newTarget();
            }
            for (Element e : mElementArray) {
                e.x += e.vx;
                e.y += e.vy;
                e.r += e.vr;
                if (e.r < 0f)   e.r += 360f;
                if (e.r > 360f) e.r -= 360f;
                if (e.type == -1) {
                    mWorkArray.add(e);
                } else if (e.type == 0) {
                    if (e.x < -mRangeX && e.vx < 0 || e.x > mRangeX && e.vx > 0) e.vx *= -1f;
                    if (e.y < -mRangeY && e.vy < 0 || e.y > mRangeY && e.vy > 0) e.vy *= -1f;
                } else {
                    e.vy -= 1 / 1024f;
                    if (e.x < -mRangeX || e.x > mRangeX || e.y < -mRangeY) {
                        e.type = -1;
                    }
                }
            }
            for (Element e : mWorkArray) {
                mElementArray.remove(e);
            }
            mWorkArray.clear();
            if (mFlashLevel > 0f) {
                mFlashLevel -= 1f / 16f;
            }
        }
    }

    public void newTarget() {
        synchronized (mElementArray) {
            Element e = new Element();
            e.vx = (float) ((Math.random() - 0.5) / 64.0);
            e.vy = (float) ((Math.random() - 0.5) / 64.0);
            e.vr = (float) ((Math.random() - 0.5) * 4.0);
            e.x = (float) ((Math.random() - 0.5) * mRangeX * 2.0);
            e.y = (float) ((Math.random() - 0.5) * mRangeY * 2.0);;
            e.r = (float) (Math.random() * 90.0);
            e.type = 0;
            if (mSpeedTrick) {
                double deg = Math.random() * Math.PI * 2.0;
                e.vx = (float) (Math.cos(deg) / 16.0);
                e.vy = (float) (Math.sin(deg) / 16.0);
            }
            if (mRotationTrick) {
                e.vr = (e.vx < 0) ? -10 : 10;
            }
            if (mAngleTrick) {
                if (Math.random() < 0.5) {
                    e.vx = 0f;
                } else {
                    e.vy = 0f;
                }
                e.vr = 0f;
            }
            mElementArray.add(e);
        }
    }

    public int judgeTarget(float x, float y) {
        int ret = 0;
        synchronized (mElementArray) {
            for (Element e : mElementArray) {
                if (e.type == 0 && Math.hypot(x - e.x, y - e.y) < HIT_SIZE) {
                    destroyTarget(e);
                    ret++;
                }
            }
            mElementArray.addAll(mWorkArray);
            mWorkArray.clear();
        }
        return ret;
    }

    public int throwBomb() {
        int ret = 0;
        synchronized (mElementArray) {
            for (Element e : mElementArray) {
                if (e.type == 0) {
                    destroyTarget(e);
                    ret++;
                }
            }
            mElementArray.addAll(mWorkArray);
            mWorkArray.clear();
        }
        if (ret > 0) {
            mFlashLevel = 1.0f;
        }
        return ret;
    }

    public ArrayList<Element> getArrayList() {
        return mElementArray;
    }

    public float getFlashLevel() {
        return mFlashLevel;
    }

    /*-----------------------------------------------------------------------*/

    private void destroyTarget(Element e) {
        for (int i = 1; i <= 16; i++) {
            if (i % 3 != 1 || i > 4 && i < 13) {
                Element p = new Element();
                p.vx = (float) ((Math.random() - 0.5) / 16.0);
                p.vy = (float) ((Math.random() - 0.5) / 16.0);
                p.vr = (float) ((Math.random() - 0.5) * 32.0);
                p.x = e.x + p.vx;
                p.y = e.y + p.vy;
                p.r = e.r + p.vr;
                p.type = i;
                mWorkArray.add(p);
            }
        }
        e.type = -1;
    }

}
