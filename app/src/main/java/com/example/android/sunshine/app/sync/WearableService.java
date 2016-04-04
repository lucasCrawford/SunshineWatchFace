package com.example.android.sunshine.app.sync;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.android.sunshine.app.Constants;
import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

/**
 * Created by lcrawford on 4/3/16.
 */
public class WearableService extends WearableListenerService implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks{

    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "WearableService";
    private static final long TIMEOUT_MS = 6000;

    private static final String[] WEARABLE_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_WEATHER_DATE = 1;
    private static final int INDEX_MAX_TEMP = 2;
    private static final int INDEX_MIN_TEMP = 3;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Created the wearable listener for listening for messages from the wearable to the phone...");
        initApiClient();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "Message received: " + messageEvent.getPath());
        if(messageEvent.getPath().equals(Constants.PATH)){
            mGoogleApiClient.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if(mGoogleApiClient.isConnected()){
                Log.d(TAG, "Sending the weather update...");

                /* Get today's weather information */
                String locationQuery = Utility.getPreferredLocation(WearableService.this);
                Uri uri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());
                Cursor cursor = getContentResolver().query(uri, WEARABLE_WEATHER_PROJECTION, null, null, null);

                /* Move cursor to front and send the data to utility method */
                if (cursor != null && cursor.moveToFirst()) {
                    int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                    long date = cursor.getLong(INDEX_WEATHER_DATE);
                    double high = cursor.getDouble(INDEX_MAX_TEMP);
                    double low = cursor.getDouble(INDEX_MIN_TEMP);
                    ContentValues todaysContent = new ContentValues();
                    todaysContent.put(WeatherContract.WeatherEntry.COLUMN_DATE, date);
                    todaysContent.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                    todaysContent.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                    todaysContent.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);
                    WearableUtils.sendDataToWatchface(WearableService.this, mGoogleApiClient, todaysContent);
                }else{
                    Log.d(TAG, "Failed to move cursor!");
                }

                /* Close cursor if necessary */
                if(cursor != null){
                    cursor.close();
                }

            }else{
                Log.d(TAG, "Failed to send weather update due to api client not being connected...");
            }
        }
    }

    /**
     * Create GoogleApiClient and connect it.
     */
    private void initApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(WearableService.this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Kill the API client and disconnect it
     */
    private void killApiClient(){
        if(mGoogleApiClient != null){
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected to GooglePlayServices!");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection suspended...!");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed");
    }
}
