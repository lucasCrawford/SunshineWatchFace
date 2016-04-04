package com.example.hercules.wearable.utils;

import com.example.hercules.wearable.R;

/**
 * Created by lcrawford on 4/3/16.
 */
public class Utility {

    public static int getArtResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.mipmap.art_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.mipmap.art_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.mipmap.art_rain;
        } else if (weatherId == 511) {
            return R.mipmap.art_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.mipmap.art_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.mipmap.art_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.mipmap.art_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.mipmap.art_storm;
        } else if (weatherId == 800) {
            return R.mipmap.art_clear;
        } else if (weatherId == 801) {
            return R.mipmap.art_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.mipmap.art_clouds;
        }
        return -1;
    }
}
