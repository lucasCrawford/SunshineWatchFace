package com.example.hercules.wearable.services;

import android.content.Intent;
import android.util.Log;

import com.example.hercules.wearable.utils.Constants;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Hercules on 3/26/2016.
 */
public class WeatherUpdateService extends WearableListenerService {

    private static final String TAG = "WeatherUpdateService";


    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "Create.");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents){
        Log.e(TAG, "HERE!");
        /* Look through the data events sent via Application */
        for (DataEvent dataEvent : dataEvents){

            if(dataEvent.getType() == DataEvent.TYPE_CHANGED){
                DataItem dataItem = dataEvent.getDataItem();
                DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                String path = dataItem.getUri().getPath();

                /* If the path of the data event is for a weather update, send broadcast */
                if(path.equals(Constants.PATH)){
                    sendWeatherUpdateBroadcast(dataMap.getFloat(Constants.DATA_HIGH_TEMP), dataMap.getFloat(Constants.DATA_LOW_TEMP));
                }
          }
        }
    }

    /* Update the watch face with the weather update details */
    private void sendWeatherUpdateBroadcast(Float high, Float low){
        Intent intent = new Intent();
        intent.putExtra(Constants.DATA_HIGH_TEMP, high);
        intent.putExtra(Constants.DATA_LOW_TEMP, low);
        intent.setAction(Constants.WEATHER_UPDATE);
        sendBroadcast(intent);
    }
}
