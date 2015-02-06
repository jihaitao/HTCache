/**   
 * @Title: HTCache.java
 * @Package org.afinal.simplecache
 * @Description: TODO(用一句话描述该文件做什么)
 * @author jihaitao@weloment.com
 * @date 2015年2月5日 上午10:11:29
 * @version V1.0
 */
package org.afinal.simplecache;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/** 
 * @ClassName: HTCache 
 * @Description: 文件缓冲管理类
 * @author jihaitao@weloment.com
 * @date 2015年2月5日 上午10:11:29 
 *  
 */
public class HTCache {
    private static final int EXPIRE_HOUR  = 60 * 60 * 1000;//过期时间
    private static final int MAX_SIZE= 1000*1000*50; //50M
    private static final int MAX_COUNT = Integer.MAX_VALUE;
    private static Map<String, HTCache> mInstanceMap = new HashMap<String, HTCache>();
    private HTCacheManager mCacheManager;
    
    public static HTCache get(Context cxt){
       return get(cxt,"HTCache");
    }
    
    public static HTCache get(Context cxt,String cacheName){
        File file = new File(cxt.getCacheDir(), cacheName);
        return get(file, MAX_SIZE, MAX_COUNT);
    }
    
    public static HTCache get(File cacheDir,long maxsize,int maxcount){
        HTCache mCache = mInstanceMap.get(cacheDir.getAbsolutePath()+mypid());
        if (mCache == null) {
            mCache = new HTCache(cacheDir, maxsize, maxcount);
            mInstanceMap.put(cacheDir.getAbsolutePath()+mypid(), mCache);
        }
        return mCache;
    }

    private HTCache(File cacheDir,long maxsize,int maxcount) {
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw new RuntimeException("cat't make dirs in " + cacheDir.getAbsolutePath());
        }
        mCacheManager = new HTCacheManager(cacheDir, maxsize, maxcount);
    }
    
    
    public static String mypid(){
        return "_" + android.os.Process.myPid();
    }
    
    
    // =======================================
    // ============ String数据 读写 ==============
    // =======================================
    /**
     * 将String写入缓冲
     *
     * @param key   关键字
     * @param value 值 
     */
    public void put(String key, String value) {
        File file = mCacheManager.newFile(key);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file),1024);
            writer.write(value);
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mCacheManager.put(file);
        }
    }
    
   
    public String getAsString(String key){
        File file = mCacheManager.get(key);
        if (!file.exists()) {
            return null;
        }
        
        if (mCacheManager.isDue(file)) {
            mCacheManager.remove(file);
            return null;
        }
 
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String readString = "";
            String currentLine = null;
            while((currentLine = reader.readLine())!=null){
                readString += currentLine;
            }
            return readString;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally{
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    
    // =======================================
    // ============= 序列化 数据 读写 ===============
    // =======================================
    /**
     * 
     * @Title: put 
     * @Description: 保存序列化数据到缓冲中 
     * @param @param key
     * @param @param value
     * @param @param saveTime  保存的时间:单位  毫秒
     * @return void
     * @throws
     */
    public void put(String key,Serializable value){
        ByteArrayOutputStream baos = null;
        ObjectOutputStream  oos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(value);
            byte[] data = baos.toByteArray();
            put(key, data);
        } catch (Exception e) {
            e.printStackTrace();
        } finally{
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 
     * @Title: getAsObject 
     * @Description: 获取对象
     * @param @param key
     * @param @return 
     * @return Object
     * @throws
     */
    public Object getAsObject(String key){
        byte[] data = getAsBinary(key);
        ByteArrayInputStream bais = null;
        ObjectInputStream ois = null;
        try {
            if (data !=null) {
                bais = new ByteArrayInputStream(data);
                ois = new ObjectInputStream(bais);
                Object obj = ois.readObject();
                return obj;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    
    
    // =======================================
    // ============= byte 数据 读写 ===============
    // =======================================
    /**
     * 
     * @Title: put 
     * @Description: 保存byte数据到缓冲中
     * @param @param key
     * @param @param value 
     * @return void
     * @throws
     */
    public void put(String key,byte[] value){
        File file = mCacheManager.newFile(key);
        FileOutputStream fos = null;
        try {
            fos= new FileOutputStream(file);
            fos.write(value);
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            if (fos!=null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            mCacheManager.put(file);
        }
    }
    
    /**
     * 
     * @Title: getAsBinary 
     * @Description: TODO(这里用一句话描述这个方法的作用) 
     * @param @param key
     * @param @return 
     * @return byte[]
     * @throws
     */
    public byte[] getAsBinary(String key){
        File file = mCacheManager.get(key);
        RandomAccessFile raFile = null;
        try {
            
            if (!file.exists()) {
                return null;
            }
            
            if (mCacheManager.isDue(file)) {
                mCacheManager.remove(file);
                return null;
            }
            
            raFile = new RandomAccessFile(file, "r");
            byte[] byteArray = new byte[(int) raFile.length()];
            raFile.read(byteArray);
            return byteArray;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally{
            if (raFile != null) {
                try {
                    raFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    
    // =======================================
    // ============== bitmap 数据 读写 =============
    // =======================================
    /**
     * 
     * @Title: put 
     * @Description: TODO(这里用一句话描述这个方法的作用) 
     * @param @param key
     * @param @param bm 
     * @return void
     * @throws
     */
    public void put(String key,Bitmap bm){
        put(key, Utils.bitmap2Bytes(bm));
    }
    
    /**
     * 
     * @Title: getAsBitmap 
     * @Description: TODO(这里用一句话描述这个方法的作用) 
     * @param @param key
     * @param @return 
     * @return Bitmap
     * @throws
     */
    public Bitmap getAsBitmap(String key){
        byte[] data = getAsBinary(key);
        if (data ==null) {
            return null;
        }
        return Utils.bytes2Bitmap(data);
    }
    
    
    // =======================================
    // ============= drawable 数据 读写 =============
    // =======================================
    /**
     * 
     * @Title: put 
     * @Description: TODO(这里用一句话描述这个方法的作用) 
     * @param @param key
     * @param @param drawable 
     * @return void
     * @throws
     */
    public void put(String key,Drawable drawable){
        put(key, Utils.drawable2Bitmap(drawable));
    }
    
    /**
     * 
     * @Title: getAsDrawable 
     * @Description: 获取Drawable
     * @param @param key
     * @param @return 
     * @return Drawable
     * @throws
     */
    public Drawable getAsDrawable(String key){
        byte[] data = getAsBinary(key);
        if (data == null) {
            return null;
        }
        return Utils.bitmap2Drawable(Utils.bytes2Bitmap(data));
    }
    
    
    
    // =======================================
    // ============= JSONObject 数据 读写 ==============
    // =======================================
    /**
     * 
     * @Title: put 
     * @Description: 保存JSONObject数据 
     * @param @param key
     * @param @param jsonObject 
     * @return void
     * @throws
     */
    public void put(String key,JSONObject jsonObject){
        put(key, jsonObject.toString());
    }
    
    /**
     * 
     * @Title: getAsJSONObject 
     * @Description: 获取JSONObject数据
     * @param @param key
     * @param @return 
     * @return JSONObject
     * @throws
     */
    public JSONObject getAsJSONObject(String key){
        try {
            JSONObject object = new JSONObject(getAsString(key));
            return object;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    //==================================================
    //================== 移除缓冲 ==========================
    //==================================================
    /**
     * 
     * @Title: remove 
     * @Description: 移除缓冲内容
     * @param @param key
     * @param @return 
     * @return boolean
     * @throws
     */
    public boolean remove(String key){
        return mCacheManager.remove(key);
    }
    
    
    
    //===================================================
    //================== 流的读写操作 ========================
    //===================================================
    
    private class HTFileOutputStream extends FileOutputStream{
        
        private File file = null;

        public HTFileOutputStream(File file) throws FileNotFoundException {
            super(file);
        }
        
        public void close() throws IOException{
            super.close();
            mCacheManager.put(file);
        }
        
    } 
    
    public OutputStream put(String key) throws FileNotFoundException{
        File file = mCacheManager.newFile(key);
        return new HTFileOutputStream(file);
    }
    
    public InputStream getAsInputStream(String key) throws FileNotFoundException{
        File file = mCacheManager.get(key);
        return new FileInputStream(file);
    }
    
    public class HTCacheManager{
        private final AtomicLong cacheSize;
        private final AtomicInteger cacheCount;
        private final long sizeLimit;
        private final long countLimit;
        private final Map<File, Long> lastUsageDates = Collections.synchronizedMap(new HashMap<File, Long>());
        protected File cacheDir;
        
        private HTCacheManager(File cacheDir,long sizeLimit,int countLimit){
            this.sizeLimit = sizeLimit;
            this.countLimit = countLimit;
            this.cacheDir = cacheDir;
            cacheSize = new AtomicLong();
            cacheCount = new AtomicInteger();
            calculateCacheSizeAndCacheCount();
        }
        
        /**
         * 计算缓冲中的文件总数和总size
         *  
         */
        private void calculateCacheSizeAndCacheCount(){
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    long size = 0;
                    int count = 0;
                    File[] cachedFiles = cacheDir.listFiles();
                    if (cachedFiles != null) {
                        for (File cachedFile : cachedFiles) {
                            size += calculateSize(cachedFile);
                            count++;
                            lastUsageDates.put(cachedFile, cachedFile.lastModified());
                        }
                        cacheSize.set(size);
                        cacheCount.set(count);
                    }
                }
            }).start();
        }
        
        private long calculateSize(File file){
            return file.length();
        }
        
        /**
         * 
         * @param key 缓冲的关键字
         * @return File 返回文件
         */
        private File newFile(String key){
            return new File(cacheDir,key.hashCode() + "");
        }
        
        private File get(String key){
            File file = newFile(key);
            Long currentTime = System.currentTimeMillis();
            file.setLastModified(currentTime);
            //这个方法存在一些问题，加入这个文件不存在也会放到map中去，这样实际缓冲中的文件会比map中的文件少一个
            //要想保持map中和缓冲中的文件个数一致，则需要将下边这行注释掉，这样既可以保证Map中和缓冲中的个数一致
            lastUsageDates.put(file, currentTime);
            return file;
        }
        
        private void put(File file){
            int curCacheCount = cacheCount.get();
            long fileSize = calculateSize(file);
            while ((curCacheCount + 1 > countLimit)
                    || ((cacheSize.get() + fileSize) > sizeLimit)) {
                long freeSize = removeNext();
                cacheSize.addAndGet(-freeSize);
                cacheCount.addAndGet(-1);
            }
            
            cacheCount.addAndGet(1);
            cacheSize.addAndGet(fileSize);
            Long currentTime = System.currentTimeMillis();
            file.setLastModified(currentTime);
            lastUsageDates.put(file, currentTime);
        }
        
        /**
         * 移除更改时间最久的文件
         * 
         * @return long 文件大小
         */
        private long removeNext(){
            if (lastUsageDates.isEmpty()) {
                return 0;
            }
            
            Long oldestUsage = null;
            File oldestUsageFile = null;
            synchronized (lastUsageDates) {
                for (Entry<File, Long> entry : lastUsageDates.entrySet()) {
                    if (oldestUsageFile == null) {
                        oldestUsage = entry.getValue();
                        oldestUsageFile = entry.getKey();
                    }else{
                        if (oldestUsage > entry.getValue()) {
                            oldestUsage = entry.getValue();
                            oldestUsageFile = entry.getKey();
                        }
                    }
                }
            }
            
            long fileSize = calculateSize(oldestUsageFile);
            if (oldestUsageFile.delete()) {
                lastUsageDates.remove(oldestUsageFile);
            }
            return fileSize;
        }
        
        private boolean isDue(File file){
            if (System.currentTimeMillis() < (file.lastModified() + EXPIRE_HOUR)) {
                return false;
            }
            return true;
        }
        
        
        /**
         * 移除缓冲文件
         * 
         * @param file  
         * @return boolean 
         */
        private boolean remove(File file){
            Long fileSize = file.length();
            if (file.delete()) {
                //每次移除缓冲个数-1，大小减少移除大小
                cacheSize.addAndGet(-fileSize);
                cacheCount.addAndGet(-1);
                lastUsageDates.remove(file);
                return true;
            }
            return false;
        }
        
        private boolean remove(String key){
            File file = get(key);
            if (remove(file)) {
                return true;
            }
            return false;
        }
    }
    
    private static class Utils{
        
        private static byte[] bitmap2Bytes(Bitmap bm){
            if (bm == null) {
                return null;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 10, baos);
            return baos.toByteArray();
        }
        
        private static Bitmap bytes2Bitmap(byte[] data){
            if (data.length == 0) {
                return null;
            }
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        }
        
        private static Bitmap drawable2Bitmap(Drawable drawable){
            if (drawable == null) {
                return null;
            }
            // 取 drawable 的长宽
            int w = drawable.getIntrinsicWidth();
            int h = drawable.getIntrinsicHeight();
            // 取 drawable 的颜色格式
            Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
            // 建立对应 bitmap
            Bitmap bitmap = Bitmap.createBitmap(w, h, config);
            // 建立对应 bitmap 的画布
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, w, h);
            // 把 drawable 内容画到画布中
            drawable.draw(canvas);
            return bitmap;
        }
        
        private static Drawable bitmap2Drawable(Bitmap bm){
            if (bm == null) {
                return null;
            }
            BitmapDrawable bd = new BitmapDrawable(bm);
            bd.setTargetDensity(bm.getDensity());
            return bd;
        }
    }
    
}
