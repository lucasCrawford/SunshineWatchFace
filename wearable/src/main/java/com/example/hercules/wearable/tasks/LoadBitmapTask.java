package com.example.hercules.wearable.tasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Simple AsyncTask to load a bitmap from an asset send by a connected node
 * to the wearable.
 * Created by lcrawford on 4/3/16.
 */
public class LoadBitmapTask extends AsyncTask<Asset, Void, Bitmap> {

    private GoogleApiClient client;
    private OnBitmapLoadedCallback mLoadedCallback;
    private static final String TAG = "Utils";
    private final Integer TIMEOUT_MS = 6000;

    public LoadBitmapTask(GoogleApiClient client, OnBitmapLoadedCallback loadedCallback){
        this.client = client;
        this.mLoadedCallback = loadedCallback;
    }

    @Override
    protected Bitmap doInBackground(Asset... params) {
        Asset asset = (Asset) params[0];

        ConnectionResult result =
                client.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }

        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                client, asset).await().getInputStream();
        client.disconnect();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        mLoadedCallback.onBitmapLoaded(bitmap);
    }

    /**
     * Callback for handling when the bitmap is loaded.
     */
    public interface OnBitmapLoadedCallback{

        void onBitmapLoaded(Bitmap bitmap);
    }
}

