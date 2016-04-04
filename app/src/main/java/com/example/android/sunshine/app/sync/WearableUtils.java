package com.example.android.sunshine.app.sync;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.example.android.sunshine.app.Constants;
import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;

/**
 * Created by lcrawford on 4/3/16.
 */
public class WearableUtils {

    private static final String TAG = "WearableUtils";

    /**
     * Send the data for today's sync'd weather to the watchface.
     * @param todaysContent
     */
    public static void sendDataToWatchface(Context context, GoogleApiClient apiClient, ContentValues todaysContent){

        /* Access today's data */
        long date = todaysContent.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE);
        Double high = todaysContent.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP);
        Double low = todaysContent.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP);

        /* Get the weather id and art resource. Then resize the bitmap here in the phone than
         * handing the work off to the wearable.
         */
        int weatherId = todaysContent.getAsInteger(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID);
        int artResource = Utility.getArtResourceForWeatherCondition(weatherId);
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(), artResource);
        Integer iconSize = context.getResources().getDimensionPixelSize(R.dimen.weather_icon_size);
        icon = Bitmap.createScaledBitmap(icon, iconSize, iconSize, false);

        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Constants.PATH);
        DataMap map = putDataMapRequest.getDataMap();

        // Uncomment this to enable debugging. Without this, only updates with changes are actually received by the wearable.
    //    map.putLong("timestamp", System.currentTimeMillis());

        /* Attach today's data for the weather */
        map.putLong(Constants.DATA_DATE, date);
        map.putFloat(Constants.DATA_HIGH_TEMP, high.floatValue());
        map.putFloat(Constants.DATA_LOW_TEMP, low.floatValue());
        map.putInt(Constants.DATA_WEATHER_ID, weatherId);
        map.putAsset(Constants.DATA_ICON, createAssetFromBitmap(icon));

        /* Send the data map through the wearable data api */
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(apiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        Log.d(TAG, "Result: " + dataItemResult);
                    }
                });
    }

    /**
     * Create an asset from a bitmap to send to the watchface.
     * @param bitmap
     * @return
     */
    public static Asset createAssetFromBitmap(Bitmap bitmap){
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return Asset.createFromBytes(baos.toByteArray());
    }

}
