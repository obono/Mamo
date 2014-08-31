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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class HistoryActivity extends Activity implements OnItemClickListener {

    private GridView        mGridView;
    private LayoutParams    mLayoutParams;
    private ArrayList<Item> mItemList = new ArrayList<Item>();

    /*-----------------------------------------------------------------------*/

    class Item {
        public boolean  mIsCurrent = false;
        public String   mFname;
        public Bitmap   mBitmap;
    }

    class MyAdapter extends ArrayAdapter<Item> {

        public MyAdapter(Context context, List<Item> list) {
            super(context, 0, list);
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            Item item = getItem(pos);
            ImageView iv = (ImageView) convertView;
            if (iv == null) {
                iv = new ImageView(getContext());
                iv.setLayoutParams(mLayoutParams);
                iv.setPadding(8, 8, 8, 8);
            }
            iv.setImageBitmap(item.mBitmap);
            return iv;
        }
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isCurrent;
        isCurrent = !addItemFunc(TargetUtils.getTargetFileName(), true);
        for (int i = 0; i < 10; i++) {
            addItemFunc(TargetUtils.getHistoryFileName(i), false);
        }
        addItemFunc(null, isCurrent);

        int size = TargetUtils.getScreenSize(this) / 3;
        mLayoutParams = new LayoutParams(size, size);
        mGridView = new GridView(this);
        mGridView.setNumColumns(GridView.AUTO_FIT);
        mGridView.setColumnWidth(size);
        mGridView.setAdapter(new MyAdapter(this, mItemList));
        mGridView.setOnItemClickListener(this);
        setContentView(mGridView);

    }

    @Override
    protected void onDestroy() {
        for (Item item : mItemList) {
            item.mBitmap.recycle();
        }
        super.onDestroy();
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        Item item = mItemList.get(pos);
        if (item.mIsCurrent) {
            setResult(RESULT_CANCELED);
        } else {
            File tmpFile = getFileStreamPath("tmp.img");
            if (item.mFname != null) {
                getFileStreamPath(item.mFname).renameTo(tmpFile);
            }
            String fname = TargetUtils.getTargetFileName();
            TargetUtils.pileHistoryFile(this, fname);
            if (item.mFname != null) {
                tmpFile.renameTo(getFileStreamPath(fname));
            }
            setResult(RESULT_OK);
        }
        finish();
    }

    /*-----------------------------------------------------------------------*/

    private boolean addItemFunc(String fname, boolean isCurrent) {
        if (fname != null) {
            if (!getFileStreamPath(fname).exists()) {
                return false;
            }
        }
        Item item = new Item();
        item.mIsCurrent = isCurrent;
        item.mFname = fname;
        item.mBitmap = TargetUtils.loadTargetBitmap(this, fname);
        mItemList.add(item);
        return true;
    }

}
