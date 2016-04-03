package com.example.hercules.wearable.utils;

import android.content.res.Resources;

import com.example.hercules.wearable.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by lcrawford on 3/29/16.
 */
public class TextFormatter {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault());

    public static String formatTwoDigitNumber(int hour) {
        return String.format("%02d", hour);
    }

    public static String formatDate(Date d){
        return SDF.format(d).toUpperCase();
    }

    public static String formatTemperature(Resources resources, Float temp){
        return resources.getString(R.string.format_temperature, (double) temp);
    }


}
