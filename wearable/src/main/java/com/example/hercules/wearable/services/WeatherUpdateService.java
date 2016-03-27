package com.example.hercules.wearable.services;

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
    public void onDataChanged(DataEventBuffer dataEvents){
        Log.e(TAG, "HERE!");
        /* Look through the data events sent via Application */
        for (DataEvent dataEvent : dataEvents){

            if(dataEvent.getType() == DataEvent.TYPE_CHANGED){
                DataItem dataItem = dataEvent.getDataItem();
                DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                String path = dataItem.getUri().getPath();
                if(path.equals(Constants.PATH)){
                    String value = dataMap.getString(Constants.DATA_VALUE);
                    Log.e(TAG, "Value: " + value);
                }
          }
        }
    }
}
