/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.hercules.wearable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.example.hercules.wearable.tasks.GetNodesTask;
import com.example.hercules.wearable.tasks.LoadBitmapTask;
import com.example.hercules.wearable.utils.Constants;
import com.example.hercules.wearable.utils.DataCache;
import com.example.hercules.wearable.utils.TextFormatter;
import com.example.hercules.wearable.utils.Utility;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class WeatherWatchFace extends CanvasWatchFaceService{
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Update rate for requesting data from the phone.
     */
    private static final long THREE_HOURS_MS = 3 * 60 * 60 * 1000;

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /** Alpha value for drawing time when in mute mode. */
    static final int MUTE_ALPHA = 100;

    /** Alpha value for drawing time when not in mute mode. */
    static final int NORMAL_ALPHA = 255;

    static final String COLON = ":";


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    /**
     * Custom handler for handling the updates
     */
    private static class EngineHandler extends Handler {
        private final WeakReference<WeatherWatchFace.Engine> mWeakReference;

        public EngineHandler(WeatherWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            WeatherWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    /**
     * Custom WatchfaceService Engine that drives the UI of the watch face
     */
    private class Engine extends CanvasWatchFaceService.Engine implements
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener{
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mAmbient;
        private boolean mMute;
        private float mColonWidth;
        private Time mTime;

        /* Paint objects */
        private Paint mBackgroundPaint;
        private Paint mTimePaint;
        private Paint mDatePaint;
        private Paint mSeparatorPaint;
        private Paint mTempPaint;
        private Paint mTempLowPaint;

        /* Text displayed for each watch face component */
        private String mCurrentDate;
        private String mCurrentHigh;
        private String mCurrentLow;

        private float mLineHeight;
        private float mItemSpacing;
        private float mSmallLineHeight;
        private float mSeparatorWidth;

        /* Icon shown on watch face */
        private Bitmap mWeatherIcon;
        private Integer mWeatherIconSize;
        private GoogleApiClient mGoogleApiClient;

        private static final String TAG = "WeatherWatchFaceEngine";

        /* Handle the time zone change via broadcast receiver */
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        /* Handle receive weather update events */
        final WeatherUpdateReceiver mWeatherUpdateReceiver = new WeatherUpdateReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Float high = intent.getFloatExtra(Constants.DATA_HIGH_TEMP, 0);
                Float low = intent.getFloatExtra(Constants.DATA_LOW_TEMP, 0);
                Integer weatherId = intent.getIntExtra(Constants.DATA_WEATHER_ID, -1);
                Long date = intent.getLongExtra(Constants.DATA_DATE, System.currentTimeMillis());
                Asset icon = intent.getParcelableExtra(Constants.DATA_ICON);
                Resources resources = getResources();
                mCurrentDate = TextFormatter.formatDate(new Date(date));
                mCurrentHigh = TextFormatter.formatTemperature(resources, high);
                mCurrentLow = TextFormatter.formatTemperature(resources, low);

                /* Load the passed bitmap */
                LoadBitmapTask task = new LoadBitmapTask(mGoogleApiClient, new LoadBitmapTask.OnBitmapLoadedCallback() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap) {
                        if(mWeatherIcon != null){
                            mWeatherIcon = bitmap;
                        }
                        invalidate();
                    }
                });
                task.execute(icon);

                /* Update the data cache with latest data and update time */
                DataCache cache = DataCache.getCache(WeatherWatchFace.this);
                cache.setFloat(Constants.DATA_HIGH_TEMP, high);
                cache.setFloat(Constants.DATA_LOW_TEMP, low);
                cache.setInteger(Constants.DATA_WEATHER_ID, weatherId);
            }
        };

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            /* Set the style for the watch face */
            setWatchFaceStyle(new WatchFaceStyle.Builder(WeatherWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

            /* Initialize everything */
            Resources resources = WeatherWatchFace.this.getResources();
            initDimens(resources);
            initPaints(resources);
            initApiClient();
            initFields(resources);
        }

        /**
         * Initialize the fields that are shown as watchface components.
         * @param resources
         */
        private void initFields(Resources resources){

            /* Init the displayed fields, pulling previous data as default from the cache. */
            DataCache cache = DataCache.getCache(WeatherWatchFace.this);
            mCurrentDate = TextFormatter.formatDate(new Date());
            mCurrentHigh = TextFormatter.formatTemperature(resources, cache.getFloat(Constants.DATA_HIGH_TEMP));
            mCurrentLow = TextFormatter.formatTemperature(resources, cache.getFloat(Constants.DATA_LOW_TEMP));

            /* Request data from the phone every time the watchface is first displayed (stay consistent)  */
            //TODO: Can be optimized so it syncs less often then every time it's created?
            initDefaultWeatherIcon();
            requestData();
            mTime = new Time();
        }

        /**
         * Initialize all the dimensional fields used to position components
         * @param resources
         */
        private void initDimens(Resources resources){
            mLineHeight = resources.getDimension(R.dimen.digital_line_height);
            mItemSpacing = mLineHeight / 2;
            mSmallLineHeight = mLineHeight / 3;
            mSeparatorWidth = resources.getDimension(R.dimen.separator_width);
            mWeatherIconSize = resources.getDimensionPixelSize(R.dimen.weather_icon_size);
        }

        /**
         * Initialize all the paint objects with default colors and fonts
         * @param resources
         */
        private void initPaints(Resources resources){

            /* Paint object for the background */
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            /* Paint object for the time */
            mTimePaint = new Paint();
            mTimePaint.setColor(resources.getColor(R.color.time_color));
            mTimePaint.setAntiAlias(true);
            mTimePaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));

            /* Paint object for the date */
            mDatePaint = new Paint();
            mDatePaint.setColor(resources.getColor(R.color.date_color));
            mDatePaint.setAntiAlias(true);
            mDatePaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));

            /* Paint object for the weather status separator */
            mSeparatorPaint = new Paint();
            mSeparatorPaint.setColor(resources.getColor(R.color.date_color));
            mSeparatorPaint.setAntiAlias(true);
            mSeparatorPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
            mSeparatorPaint.setStrokeWidth(.25f);

            /* Paint object for the HIGH temp text */
            mTempPaint = new Paint();
            mTempPaint.setColor(resources.getColor(R.color.time_color));
            mTempPaint.setAntiAlias(true);
            mTempPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));

            /* Paint object for the LOW temp text */
            mTempLowPaint = new Paint();
            mTempLowPaint.setColor(resources.getColor(R.color.date_color));
            mTempLowPaint.setAntiAlias(true);
            mTempLowPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        }

        /**
         * Update the anti alias state when switching from ambient to low ambient mode and backwards.
         * @param status
         */
        private void setAntiAliasStatus(boolean status){
            mTempLowPaint.setAntiAlias(status);
            mTempPaint.setAntiAlias(status);
            mSeparatorPaint.setAntiAlias(status);
            mDatePaint.setAntiAlias(status);
            mTimePaint.setAntiAlias(status);
        }

        /**
         * Update the alpha state when updating the mute mode state.
         * @param alpha
         */
        private void setAlphaStatus(int alpha){
            mTempLowPaint.setAlpha(alpha);
            mTempPaint.setAlpha(alpha);
            mSeparatorPaint.setAlpha(alpha);
            mDatePaint.setAlpha(alpha);
            mTimePaint.setAlpha(alpha);
        }

        /**
         * Request the weather data if there is no cached data for today.
         */
        private void requestData(){
            GetNodesTask getNodesTask = new GetNodesTask(mGoogleApiClient, new GetNodesTask.OnNodesLoadedListener() {
                @Override
                public void onNodesFound(List<Node> nodes) {
                    for(final Node n : nodes){
                        Wearable.MessageApi.sendMessage(mGoogleApiClient, n.getId(), Constants.PATH,
                                new byte[0]).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                if (Log.isLoggable(TAG, Log.DEBUG)) {
                                    if (!sendMessageResult.getStatus().isSuccess()) {
                                        Log.d(TAG, "Failed to send successful message request for weather!");
                                    } else {
                                        Log.d(TAG, "Successfully sent message to connect node: " + n.getDisplayName());
                                    }
                                }
                            }
                        });
                    }
                }
            });
            getNodesTask.execute();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            killApiClient();
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

            /* Added just incase later on we add some emboldened text. Not used ATM */
            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));
            }
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = WeatherWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            float dateTextSize = resources.getDimension(isRound
                    ? R.dimen.digital_date_text_size : R.dimen.digital_date_text_size);
            float subTextSize = resources.getDimension(R.dimen.digital_sub_text_size);

            mDatePaint.setTextSize(dateTextSize);
            mTimePaint.setTextSize(textSize);
            mTempPaint.setTextSize(subTextSize);
            mTempLowPaint.setTextSize(subTextSize);
            mColonWidth = mTimePaint.measureText(COLON);
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    setAntiAliasStatus(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /*
            TODO: Optimize this so it isn't doing so much math in onDraw
         */
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);
            }

            // Format the time.
            String timeFormatted =
                    TextFormatter.formatTwoDigitNumber(mTime.hour)
                    + COLON
                    + TextFormatter.formatTwoDigitNumber(mTime.minute);

            /* Get initial positions based on size of the screen */
            int xPos = (canvas.getWidth() / 2);
            int yPos = (int) ((canvas.getHeight() / 2) - ((mTimePaint.descent() + mTimePaint.ascent()) / 2)) ;
            yPos -= mLineHeight * 2;

            /* Calculate the time and date x-pos */
            int timeX = xPos - (int) (mTimePaint.measureText(timeFormatted) / 2);
            int dateX = xPos - (int) (mDatePaint.measureText(mCurrentDate)) / 2;

            // Draw the updated time
            canvas.drawText(timeFormatted, timeX, yPos, mTimePaint);

            // Draw the date
            yPos += mItemSpacing - ((mDatePaint.descent() + mDatePaint.ascent()) / 2);
            canvas.drawText(mCurrentDate, dateX, yPos, mDatePaint);

            // Draw the separator
            yPos += mItemSpacing;
            int lineX = xPos - (int) ((3*mSeparatorWidth)/4);
            canvas.drawLine(lineX, yPos, lineX + mTempPaint.measureText(mCurrentHigh), yPos, mSeparatorPaint);

            // Draw the high-temp
            yPos += mItemSpacing - ((mTempPaint.descent() + mTempPaint.ascent()) / 2);
            canvas.drawText(mCurrentHigh, xPos - (int) (mTempPaint.measureText(mCurrentHigh) / 2), yPos + mSmallLineHeight, mTempPaint);

            // Draw the low-temp
            int xPosLowTemp = xPos + (int) (mSmallLineHeight);
            canvas.drawText(mCurrentLow, xPosLowTemp + (int) (mTempLowPaint.measureText(mCurrentLow) / 2), yPos + mSmallLineHeight, mTempLowPaint);

            // Draw the bitmap
            int xPosBitmap = xPos - (int) (3*mLineHeight/4) - mWeatherIcon.getWidth();
            canvas.drawBitmap(mWeatherIcon, xPosBitmap, (yPos) - mWeatherIcon.getHeight() / 2, null);
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onInterruptionFilterChanged: " + interruptionFilter);
            }
            super.onInterruptionFilterChanged(interruptionFilter);

            /* We only need to update once a minute in mute mode. */
            boolean inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE;
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                int alpha = inMuteMode ? MUTE_ALPHA : NORMAL_ALPHA;
                setAlphaStatus(alpha);
                invalidate();
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceivers();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceivers();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * User this default weather icon when data hasn't been synced properly by the watch and phone
         */
        private void initDefaultWeatherIcon(){
            DataCache dataCache = DataCache.getCache(WeatherWatchFace.this);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;

            int weatherId = dataCache.getInteger(Constants.DATA_WEATHER_ID);
            int weatherIcon = Utility.getArtResourceForWeatherCondition(weatherId);
            if(weatherIcon == -1){
                weatherIcon = R.mipmap.art_clear;
            }
            mWeatherIcon = BitmapFactory.decodeResource(getResources(), weatherIcon, options);
            mWeatherIcon = Bitmap.createScaledBitmap(mWeatherIcon, mWeatherIconSize, mWeatherIconSize, false);
        }

        /**
         * Register all of the receiver objects for listening to particular broadcasts
         */
        private void registerReceivers() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }

            /* Register the time zone receiver */
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WeatherWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);

            /* Register the update receiver */
            IntentFilter weatherFilter = new IntentFilter(Constants.WEATHER_UPDATE);
            WeatherWatchFace.this.registerReceiver(mWeatherUpdateReceiver, weatherFilter);
        }

        /**
         * Unregister all the broadcast receivers.
         */
        private void unregisterReceivers() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WeatherWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
            WeatherWatchFace.this.unregisterReceiver(mWeatherUpdateReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        /**
         * Connect to the Google API client
         */
        private void initApiClient(){
            mGoogleApiClient = new GoogleApiClient.Builder(WeatherWatchFace.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
        }

        /**
         * Kill the Google API client by disconnecting
         */
        private void killApiClient(){
            if(mGoogleApiClient != null){
                mGoogleApiClient.disconnect();
            }
        }


        @Override
        public void onConnected(Bundle bundle) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Connected to GooglePlayServices!");
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Connection suspended...!");
            }
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Connection failed");
            }
        }
    }

    /**
     * Placeholder class that is used to receive weather update events. I prefer having a nice name
     * to the receiver for readability.
     */
    public abstract class WeatherUpdateReceiver extends BroadcastReceiver {
        @Override
        public abstract void onReceive(Context context, Intent intent);
    }

}
