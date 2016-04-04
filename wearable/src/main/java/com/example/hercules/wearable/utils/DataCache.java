package com.example.hercules.wearable.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcelable;
import android.preference.PreferenceManager;

/**
 * Created by Hercules on 4/2/2016.
 */
public class DataCache {

    private static DataCache mDataCache;
    private static SharedPreferences mSharedPrefs;

    public DataCache(){
    }

    public static DataCache getCache(Context context){
        if(mDataCache == null){
            mDataCache = new DataCache();
            mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }
        return mDataCache;
    }

    public void setFloat(String key, Float d){
        mSharedPrefs.edit().putFloat(key, d).apply();
    }

    public Float getFloat(String key){
        return mSharedPrefs.getFloat(key, 0f);
    }

    public void setLong(String key, Long l){
        mSharedPrefs.edit().putLong(key, l).apply();
    }

    public Long getLong(String key){
        return mSharedPrefs.getLong(key, -1L);
    }

    public void setInteger(String key, Integer i){
        mSharedPrefs.edit().putInt(key, i).apply();
    }

    public Integer getInteger(String key){
        return mSharedPrefs.getInt(key, -1);
    }
}
