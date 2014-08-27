package com.obnsoft.mamo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

public class TargetUtils {

    private static final String FNAME_TARGET = "target.img";

    public static String getTargetFileName() {
        return FNAME_TARGET;
    }

    public static String getHistoryFileName(int num) {
        return String.valueOf(num).concat(".img");
    }

    public static Bitmap loadTargetBitmap(Context context, String fname) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        if (fname != null) {
            try {
                InputStream in = context.openFileInput(fname);
                bitmap = BitmapFactory.decodeStream(in, null, options);
                in.close();
            } catch (FileNotFoundException e) {
                //e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(
                    context.getResources(), R.drawable.obono256, options);
        }
        bitmap.setHasAlpha(true);
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int cx = w / 2, cy = h / 2, size = Math.min(cx, cy);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (Math.hypot(x - cx, y - cy) > size) {
                    bitmap.setPixel(x, y, Color.TRANSPARENT);
                }
            }
        }
        return bitmap;
    }

    public static void pileHistoryFile(Context context, String fname) {
        char c = fname.charAt(0);
        int num = (c >= '0' && c <= '9') ? c - '0' : -1;
        File srcFile = context.getFileStreamPath(fname);
        if (srcFile.exists()) {
            if (num >= 9) {
                srcFile.delete();
            } else {
                String destFname = getHistoryFileName(num + 1);
                File destFile = context.getFileStreamPath(destFname);
                if (destFile.exists()) {
                    pileHistoryFile(context, destFname);
                }
                srcFile.renameTo(destFile);
            }
        }
    }

    public static int getScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display disp = wm.getDefaultDisplay();
        Point dispSize = new Point();
        disp.getSize(dispSize);
        return Math.min(dispSize.x, dispSize.y);
    }
}
