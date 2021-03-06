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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {

    private static final int REQUEST_CAPTURE = 1;
    private static final String PREF_KEY_COUNT = "count";
    private static final String PREF_KEY_BOMB = "bomb";
    private static final String PREF_KEY_SOUND = "sound";
    private static final String PREF_KEY_LAST = "last_launch";
    private static final String INTENT_EXTRA_SIMPLE = "simple_mode";

    private static ElementsManager  sManager = new ElementsManager();

    private SharedPreferences   mPrefs;
    private GLSurfaceView       mGLView;
    private RelativeLayout      mGroupUI;
    private MyRenderer          mRenderer;
    private TextView            mCountTextView;
    private ImageButton         mBombButton;
    private TextView            mBombTextView;
    private ImageView           mSoundIconView;
    private AdView              mAdView;
    private TextView            mAdTextView;
    private SensorManager       mSensorMan;
    private Sensor              mSensor;
    private SoundPool           mSoundPool;
    private int[]               mSoundId = new int[5];

    private boolean             mSimpleMode;
    private int                 mCount;
    private int                 mBomb;
    private boolean             mSound;
    private long                mLaunchTime;
    private boolean             mAdLoaded;

    /*-----------------------------------------------------------------------*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mSimpleMode = false;
        if (intent != null && intent.getBooleanExtra(INTENT_EXTRA_SIMPLE, false)) {
            mSimpleMode = true;
            int orientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            switch (getResources().getConfiguration().orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                orientation = (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) ?
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                orientation = (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270) ?
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT :
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                break;
            }
            setRequestedOrientation(orientation);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.main);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mCount = mPrefs.getInt(PREF_KEY_COUNT, 0);
        mBomb = mPrefs.getInt(PREF_KEY_BOMB, 10);
        mSound = mPrefs.getBoolean(PREF_KEY_SOUND, false);

        Calendar cal = Calendar.getInstance();
        mLaunchTime = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long lastLaunch = mPrefs.getLong(PREF_KEY_LAST, 0);
        if (lastLaunch > 0 && lastLaunch < cal.getTimeInMillis()) {
            obtainBombs(3);
        }

        int day = cal.get(Calendar.DAY_OF_YEAR);
        sManager.setInterval((day == 16) ? 8 : Math.abs(day % 21 - 10) + 50);
        sManager.setTricks((day % 67 == 33), (day % 31 == 11), (day % 37 == 22));

        mGLView = (GLSurfaceView) findViewById(R.id.glview);
        mGroupUI = (RelativeLayout) findViewById(R.id.group_ui);
        mRenderer = new MyRenderer(this, sManager);
        mGLView.setRenderer(mRenderer);
        mGLView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    float w = view.getWidth();
                    float h = view.getHeight();
                    float s = Math.min(w, h);
                    int p = event.getActionIndex();
                    float x = (event.getX(p) - w / 2) / s;
                    float y = (h / 2 - event.getY(p)) / s;
                    int count = sManager.judgeTarget(x, y);
                    if (count > 0) {
                        mCount += count;
                        updateCount();
                        if (mSound) {
                            mSoundPool.play(mSoundId[(int) (Math.random() * 4.0) + 1], 1f, 1f,
                                    1, 0, (float) (Math.random() * 0.75 + 0.75));
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                }
                return true;
            }
        });
        mCountTextView = (TextView) findViewById(R.id.text_count);
        mBombButton = (ImageButton) findViewById(R.id.btn_bomb);
        mBombTextView = (TextView) findViewById(R.id.text_bomb);
        mSoundIconView = (ImageView) findViewById(R.id.img_sound);
        mAdView = (AdView) findViewById(R.id.ad);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                mAdLoaded = true;
            }
            @Override
            public void onAdOpened() {
                super.onAdOpened();
                obtainBombs((int) (Math.sqrt(Math.random()) * 11.0) + 5);
                updateBomb();
                updateAdRequest();
            }
        });
        mAdTextView = (TextView) findViewById(R.id.text_ad);

        updateCount();
        updateBomb();
        updateSoundIcon();
        if (mSimpleMode) {
            mGroupUI.setVisibility(View.INVISIBLE);
        }

        mSensorMan = (SensorManager) getSystemService(SENSOR_SERVICE);
        if ((mSensorMan.getSensors() & SensorManager.SENSOR_ACCELEROMETER) != 0) {
            mSensor = mSensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        mSoundId[0] = mSoundPool.load(this, R.raw.bomb, 1);
        mSoundId[1] = mSoundPool.load(this, R.raw.crash1, 1);
        mSoundId[2] = mSoundPool.load(this, R.raw.crash2, 1);
        mSoundId[3] = mSoundPool.load(this, R.raw.crash3, 1);
        mSoundId[4] = mSoundPool.load(this, R.raw.crash4, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mSensorMan.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);
        if (!mSimpleMode && !mAdLoaded) {
            updateAdRequest();
        }
    }

    @Override
    protected void onPause() {
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mSensorMan.unregisterListener(this);
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(PREF_KEY_COUNT, mCount);
        editor.putInt(PREF_KEY_BOMB, mBomb);
        editor.putBoolean(PREF_KEY_SOUND, mSound);
        editor.putLong(PREF_KEY_LAST, mLaunchTime);
        editor.commit();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mSimpleMode) {
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_camera:
            startActivityForResult(new Intent(this, CaptureCameraActivity.class), REQUEST_CAPTURE);
            return true;
        case R.id.menu_gallery:
            startActivityForResult(new Intent(this, CaptureGalleryActivity.class), REQUEST_CAPTURE);
            return true;
        case R.id.menu_history:
            startActivityForResult(new Intent(this, HistoryActivity.class), REQUEST_CAPTURE);
            return true;
        case R.id.menu_simple_mode:
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(INTENT_EXTRA_SIMPLE, true);
            startActivity(intent);
            finish();
            return true;
        case R.id.menu_about:
            showVersion();
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CAPTURE:
            if (resultCode == RESULT_OK) {
                mRenderer.setToReloadTexture();
            }
            break;
        }
    }

    public void onClickBomb(View v) {
        if (mBomb > 0) {
            int count = sManager.throwBomb();
            if (count > 0) {
                mCount += count;
                updateCount();
                mBomb--;
                updateBomb();
                if (mSound) {
                    mSoundPool.play(mSoundId[0], 1f, 1f, 1, 0, 1f);
                }
            }
        }
    }

    public void onClickSound(View v) {
        mSound = !mSound;
        updateSoundIcon();
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing.
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float v0 = event.values[0];
        float v1 = event.values[1];
        float v2 = event.values[2];
        float r = v0 * v0 + v1 * v1 + v2 * v2;
        if (r > 400f) {
            sManager.newTarget();
        }
    }

    /*-----------------------------------------------------------------------*/

    private void updateCount() {
        mCountTextView.setText(String.valueOf(mCount));
    }

    private void updateBomb() {
        mBombTextView.setText(String.valueOf(mBomb));
        mBombButton.setEnabled((mBomb > 0));
        mAdTextView.setVisibility((mBomb == 0) ? View.VISIBLE : View.GONE);
    }

    private void updateSoundIcon() {
        mSoundIconView.setImageResource(mSound ? android.R.drawable.ic_lock_silent_mode_off :
            android.R.drawable.ic_lock_silent_mode);
    }

    private void updateAdRequest() {
        mAdLoaded = false;
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("5DD43132EC7E22D2300FE1176597CFBA")
                .build();
        mAdView.loadAd(adRequest);
    }

    private void obtainBombs(int num) {
        mBomb += num;
        Toast.makeText(this, String.format(getString(R.string.msg_obtain_bomb), num),
                Toast.LENGTH_LONG).show();
    }

    private void showVersion() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View aboutView = inflater.inflate(R.layout.about, new ScrollView(this));
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(), PackageManager.GET_META_DATA);
            TextView textView = (TextView) aboutView.findViewById(R.id.text_about_version);
            textView.setText("Version " + packageInfo.versionName);

            StringBuilder buf = new StringBuilder();
            InputStream in = getResources().openRawResource(R.raw.license);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String str;
            while((str = reader.readLine()) != null) {
                buf.append(str).append('\n');
            }
            textView = (TextView) aboutView.findViewById(R.id.text_about_message);
            textView.setText(buf.toString());
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.menu_about)
                .setView(aboutView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

}
