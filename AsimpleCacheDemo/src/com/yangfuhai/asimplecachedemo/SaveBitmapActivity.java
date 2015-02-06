package com.yangfuhai.asimplecachedemo;

import org.afinal.simplecache.HTCache;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * 
 * @ClassName: SaveBitmapActivity
 * @Description: 缓存bitmap
 * @Author Yoson Hao
 * @WebSite www.haoyuexing.cn
 * @Email haoyuexing@gmail.com
 * @Date 2013-8-7 下午5:20:37
 * 
 */
public class SaveBitmapActivity extends Activity {

    private ImageView mIv_bitmap_res;

    private HTCache mHTCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_bitmap);
        // 初始化控件
        initView();

        mHTCache = HTCache.get(this);
    }

    /**
     * 初始化控件
     */
    private void initView() {
        mIv_bitmap_res = (ImageView) findViewById(R.id.iv_bitmap_res);
    }

    /**
     * 点击save事件
     * 
     * @param v
     */
    public void save(View v) {
        Resources res = getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(res, R.drawable.img_test);
        mHTCache.put("testBitmap", bitmap);
    }

    /**
     * 点击read事件
     * 
     * @param v
     */
    public void read(View v) {
        Bitmap testBitmap = mHTCache.getAsBitmap("testBitmap");
        if (testBitmap == null) {
            Toast.makeText(this, "Bitmap cache is null ...", Toast.LENGTH_SHORT)
                    .show();
            mIv_bitmap_res.setImageBitmap(null);
            return;
        }
        mIv_bitmap_res.setImageBitmap(testBitmap);
    }

    /**
     * 点击clear事件
     * 
     * @param v
     */
    public void clear(View v) {
        mHTCache.remove("testBitmap");
    }
}
