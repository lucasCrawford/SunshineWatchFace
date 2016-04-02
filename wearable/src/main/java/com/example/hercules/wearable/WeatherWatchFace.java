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

import com.example.hercules.wearable.utils.Constants;
import com.example.hercules.wearable.utils.TextFormatter;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
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
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /** Alpha value for drawing time when in mute mode. */
    static final int MUTE_ALPHA = 100;

    /** Alpha value for drawing time when not in mute mode. */
    static final int NORMAL_ALPHA = 255;


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
    private class Engine extends CanvasWatchFaceService.Engine{
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        boolean mAmbient;
        boolean mMute;
        float mColonWidth;
        Time mTime;

        /* Paint objects */
        Paint mBackgroundPaint;
        Paint mHandPaint;
        Paint mDatePaint;
        Paint mSeparatorPaint;
        Paint mTempPaint;
        Paint mTempLowPaint;

        private static final String TAG = "WeatherWatchFaceEngine";

        /* Handle the time zone change via broadcast receiver */
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        final WeatherUpdateReceiver mWeatherUpdateReceiver = new WeatherUpdateReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e(TAG, "Received weather update!");
            }
        };

        float mXOffset;
        float mYOffset;
        float mLineHeight;
        float mSeparatorWidth;
        Bitmap mWeatherIcon;
        Integer mWeatherIconSize;

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

            Resources resources = WeatherWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mLineHeight = resources.getDimension(R.dimen.digital_line_height);
            mSeparatorWidth = resources.getDimension(R.dimen.separator_width);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mHandPaint = new Paint();
            mHandPaint.setColor(resources.getColor(R.color.time_color));
            mHandPaint.setAntiAlias(true);
            mHandPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));

            mDatePaint = new Paint();
            mDatePaint.setColor(resources.getColor(R.color.date_color));
            mDatePaint.setAntiAlias(true);
            mDatePaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));

            mSeparatorPaint = new Paint();
            mSeparatorPaint.setColor(resources.getColor(R.color.date_color));
            mSeparatorPaint.setAntiAlias(true);
            mSeparatorPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
            mSeparatorPaint.setStrokeWidth(.25f);


            mTempPaint = new Paint();
            mTempPaint.setColor(resources.getColor(R.color.time_color));
            mTempPaint.setAntiAlias(true);
            mTempPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));

            mTempLowPaint = new Paint();
            mTempLowPaint.setColor(resources.getColor(R.color.date_color));
            mTempLowPaint.setAntiAlias(true);
            mTempLowPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            mWeatherIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.art_clear, options);
            mWeatherIconSize = getResources().getDimensionPixelSize(R.dimen.weather_icon_size);
            mWeatherIcon = Bitmap.createScaledBitmap(mWeatherIcon, mWeatherIconSize, mWeatherIconSize, false);

            mWeatherIconSize = resources.getDimensionPixelSize(R.dimen.weather_icon_size);
            mTime = new Time();
        }


        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            mHandPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));

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
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            float dateTextSize = resources.getDimension(isRound
                    ? R.dimen.digital_date_text_size : R.dimen.digital_date_text_size);
            float subTextSize = resources.getDimension(R.dimen.digital_sub_text_size);

            mDatePaint.setTextSize(dateTextSize);
            mHandPaint.setTextSize(textSize);
            mTempPaint.setTextSize(subTextSize);
            mTempLowPaint.setTextSize(subTextSize);
            mColonWidth = mHandPaint.measureText(":");
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHandPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);
            }

            String result = TextFormatter.formatTwoDigitNumber(mTime.hour)+":"+ TextFormatter.formatTwoDigitNumber(mTime.minute);
            String date = TextFormatter.formatDate(new Date());

            int xPos = (canvas.getWidth() / 2);
            int yPos = (int) ((canvas.getHeight() / 2) - ((mHandPaint.descent() + mHandPaint.ascent()) / 2)) ;
            yPos -= mLineHeight * 2;

            int timeX = xPos - (int) (mHandPaint.measureText(result) / 2);
            int dateX = xPos - (int) (mDatePaint.measureText(date)) / 2;

            // Draw the time
            canvas.drawText(result, timeX, yPos, mHandPaint);

            // Draw the date
            yPos += (mLineHeight / 2) - ((mDatePaint.descent() + mDatePaint.ascent()) / 2);
            canvas.drawText(date, dateX, yPos, mDatePaint);

            // Draw the separator
            String temp = getResources().getString(R.string.format_temperature, 25f);
            yPos += (mLineHeight / 2);
            int lineX = xPos - (int) ((3*mSeparatorWidth)/4);
            canvas.drawLine(lineX, yPos, lineX + mTempPaint.measureText(temp), yPos, mSeparatorPaint);

            // Draw the high-temp
            yPos += (mLineHeight / 2) - ((mTempPaint.descent() + mTempPaint.ascent()) / 2);
            canvas.drawText(temp, xPos - (int) (mTempPaint.measureText(temp) / 2), yPos + mLineHeight / 3, mTempPaint);

            // Draw the low-temp
            String lowTemp = getResources().getString(R.string.format_temperature, 16f);
            int xPosLowTemp = xPos + (int) (mLineHeight / 3);
            canvas.drawText(lowTemp, xPosLowTemp + (int) (mTempLowPaint.measureText(lowTemp) / 2), yPos + mLineHeight / 3, mTempLowPaint);

            // Draw the bitmap
            int xPosBitmap = xPos - 2*((int) (mLineHeight / 2)) - mWeatherIcon.getWidth();
            canvas.drawBitmap(mWeatherIcon, xPosBitmap, (yPos) - mWeatherIcon.getHeight()/2, null);
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onInterruptionFilterChanged: " + interruptionFilter);
            }
            super.onInterruptionFilterChanged(interruptionFilter);

            boolean inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE;
            // We only need to update once a minute in mute mode.

            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                int alpha = inMuteMode ? MUTE_ALPHA : NORMAL_ALPHA;
                mHandPaint.setAlpha(alpha);
                invalidate();
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                Log.e(TAG, "Visible");
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                Log.e(TAG, "Invisible");
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /* Register receiver */
        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }

            /* Register the time zone reciever */
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WeatherWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);

            /* Register the update receiver */
            IntentFilter weatherFilter = new IntentFilter(Constants.WEATHER_UPDATE);
            WeatherWatchFace.this.registerReceiver(mWeatherUpdateReceiver, weatherFilter);
        }

        /* Unregister receiver */
        private void unregisterReceiver() {
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
    }

    public abstract class WeatherUpdateReceiver extends BroadcastReceiver{

        @Override
        public abstract void onReceive(Context context, Intent intent);
    }
}
