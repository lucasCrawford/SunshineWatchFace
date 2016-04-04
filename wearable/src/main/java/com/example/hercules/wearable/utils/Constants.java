package com.example.hercules.wearable.utils;

/**
 * Created by Hercules on 3/25/2016.
 */
public class Constants {

    /* Data item constants for App -> Wearable communication */
    public static final String PATH = "/weather-path";
    public static final String DATA_HIGH_TEMP = "DATA_HIGH_TEMP";
    public static final String DATA_LOW_TEMP = "DATA_LOW_TEMP";
    public static final String DATA_DATE = "DATA_DATE";
    public static final String DATA_ICON = "DATA_ICON";
    public static final String DATA_WEATHER_ID = "DATA_WEATHER_ID";
    public static final String DATA_LAST_UPDATE = "LAST_UPDATE";

    /* Custom broadcast receiver to update the wearable's data */
    public static final String WEATHER_UPDATE = "com.example.hercules.wearable.WEATHER_UPDATE";

}
