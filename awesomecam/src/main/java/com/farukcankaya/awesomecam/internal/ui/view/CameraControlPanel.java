package com.farukcankaya.awesomecam.internal.ui.view;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.anggrayudi.storage.media.MediaFile;
import com.farukcankaya.awesomecam.R;
import com.farukcankaya.awesomecam.internal.configuration.AwesomeCamConfiguration;
import com.farukcankaya.awesomecam.internal.utils.DateTimeUtils;

import java.util.concurrent.TimeUnit;

/**
 * Created by memfis on 7/6/16.
 */
public class CameraControlPanel extends RelativeLayout
        implements RecordButton.RecordButtonListener,
        MediaActionSwitchView.OnMediaActionStateChangeListener {

    private Context context;

    private CameraSwitchView cameraSwitchView;
    private RecordButton recordButton;
    private MediaActionSwitchView mediaActionSwitchView;
    private FlashSwitchView flashSwitchView;
    private TextView recordDurationText;
    private TextView recordSizeText;
    private CameraSettingsView settingsButton;

    private RecordButton.RecordButtonListener recordButtonListener;
    private MediaActionSwitchView.OnMediaActionStateChangeListener onMediaActionStateChangeListener;
    private CameraSwitchView.OnCameraTypeChangeListener onCameraTypeChangeListener;
    private FlashSwitchView.FlashModeSwitchListener flashModeSwitchListener;
    private SettingsClickListener settingsClickListener;

    private TimerTaskBase countDownTimer;
    private long recordDuration = 0;

    public interface SettingsClickListener {
        void onSettingsClick();
    }

    private boolean hasFlash = false;

    private
    @MediaActionSwitchView.MediaActionState
    int mediaActionState;

    private int mediaAction;

    private FileObserver fileObserver;

    public CameraControlPanel(Context context) {
        this(context, null);
    }

    public CameraControlPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        hasFlash = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
//        hasFlash = false;

        LayoutInflater.from(context).inflate(R.layout.camera_control_panel_layout, this);
        setBackgroundColor(Color.TRANSPARENT);

        settingsButton = (CameraSettingsView) findViewById(R.id.settings_view);
        cameraSwitchView = (CameraSwitchView) findViewById(R.id.front_back_camera_switcher);
        mediaActionSwitchView = (MediaActionSwitchView) findViewById(R.id.photo_video_camera_switcher);
        recordButton = (RecordButton) findViewById(R.id.record_button);
        flashSwitchView = (FlashSwitchView) findViewById(R.id.flash_switch_view);
        recordDurationText = (TextView) findViewById(R.id.record_duration_text);
        recordSizeText = (TextView) findViewById(R.id.record_size_mb_text);

        cameraSwitchView.setOnCameraTypeChangeListener(onCameraTypeChangeListener);
        mediaActionSwitchView.setOnMediaActionStateChangeListener(this);

        setOnCameraTypeChangeListener(onCameraTypeChangeListener);
        setOnMediaActionStateChangeListener(onMediaActionStateChangeListener);
        setFlashModeSwitchListener(flashModeSwitchListener);
        setRecordButtonListener(recordButtonListener);
        settingsButton.setOnClickListener(view -> {
            if (settingsClickListener != null) settingsClickListener.onSettingsClick();
        });

        if (hasFlash)
            flashSwitchView.setVisibility(VISIBLE);
        else flashSwitchView.setVisibility(GONE);

        countDownTimer = new TimerTask(recordDurationText);
    }

    public void lockControls() {
        cameraSwitchView.setEnabled(false);
        recordButton.setEnabled(false);
        settingsButton.setEnabled(false);
        flashSwitchView.setEnabled(false);
    }

    public void unLockControls() {
        cameraSwitchView.setEnabled(true);
        recordButton.setEnabled(true);
        settingsButton.setEnabled(true);
        flashSwitchView.setEnabled(true);
    }

    public void setup(int mediaAction, RecordButton.MinimumRecordCallback minimumRecordCallback) {
        this.mediaAction = mediaAction;
        if (AwesomeCamConfiguration.MEDIA_ACTION_VIDEO == mediaAction) {
            recordButton.setup(mediaAction, this, minimumRecordCallback);
            flashSwitchView.setVisibility(GONE);
        } else {
            recordButton.setup(AwesomeCamConfiguration.MEDIA_ACTION_PHOTO, this, null);
        }

        if (AwesomeCamConfiguration.MEDIA_ACTION_UNSPECIFIED != mediaAction) {
            mediaActionSwitchView.setVisibility(GONE);
        } else mediaActionSwitchView.setVisibility(VISIBLE);
    }

    public void setFlasMode(@FlashSwitchView.FlashMode int flashMode) {
        flashSwitchView.setFlashMode(flashMode);
    }

    public void setMediaFileUri(final Uri uri) {
    }

    public void setMaxVideoFileSize(long maxVideoFileSize) {
    }

    public void setMaxVideoDuration(int maxVideoDurationInMillis) {
        if (maxVideoDurationInMillis > 0)
            countDownTimer = new CountdownTask(recordDurationText, maxVideoDurationInMillis);
        else countDownTimer = new TimerTask(recordDurationText);
    }

    public void setMediaActionState(@MediaActionSwitchView.MediaActionState int actionState) {
        if (mediaActionState == actionState) return;
        if (MediaActionSwitchView.ACTION_PHOTO == actionState) {
            recordButton.setMediaAction(AwesomeCamConfiguration.MEDIA_ACTION_PHOTO);
            if (hasFlash)
                flashSwitchView.setVisibility(VISIBLE);
        } else {
            recordButton.setMediaAction(AwesomeCamConfiguration.MEDIA_ACTION_VIDEO);
            flashSwitchView.setVisibility(GONE);
        }
        mediaActionState = actionState;
        mediaActionSwitchView.setMediaActionState(actionState);
    }

    public void setRecordButtonListener(RecordButton.RecordButtonListener recordButtonListener) {
        this.recordButtonListener = recordButtonListener;
    }

    public void rotateControls(int rotation) {
        cameraSwitchView.setRotation(rotation);
        mediaActionSwitchView.setRotation(rotation);
        flashSwitchView.setRotation(rotation);
        recordDurationText.setRotation(rotation);
        recordSizeText.setRotation(rotation);
    }

    public void setOnMediaActionStateChangeListener(MediaActionSwitchView.OnMediaActionStateChangeListener onMediaActionStateChangeListener) {
        this.onMediaActionStateChangeListener = onMediaActionStateChangeListener;
    }

    public void setOnCameraTypeChangeListener(CameraSwitchView.OnCameraTypeChangeListener onCameraTypeChangeListener) {
        this.onCameraTypeChangeListener = onCameraTypeChangeListener;
        if (cameraSwitchView != null)
            cameraSwitchView.setOnCameraTypeChangeListener(this.onCameraTypeChangeListener);
    }

    public void setFlashModeSwitchListener(FlashSwitchView.FlashModeSwitchListener flashModeSwitchListener) {
        this.flashModeSwitchListener = flashModeSwitchListener;
        if (flashSwitchView != null)
            flashSwitchView.setFlashSwitchListener(this.flashModeSwitchListener);
    }

    public void setSettingsClickListener(SettingsClickListener settingsClickListener) {
        this.settingsClickListener = settingsClickListener;
    }

    @Override
    public void onTakePhotoButtonPressed() {
        if (recordButtonListener != null)
            recordButtonListener.onTakePhotoButtonPressed();
    }

    public void onStartVideoRecord(final MediaFile mediaFile) {
        setMediaFileUri(mediaFile.getUri());
        countDownTimer.start();

    }

    public void allowRecord(boolean isAllowed) {
        recordButton.setEnabled(isAllowed);
    }

    public void allowCameraSwitching(boolean isAllowed) {
        cameraSwitchView.setVisibility(isAllowed ? VISIBLE : GONE);
    }

    public long getRecordDuration() {
        return recordDuration;
    }

    public void onStopVideoRecord() {
        if (fileObserver != null)
            fileObserver.stopWatching();
        countDownTimer.stop();

        recordSizeText.setVisibility(GONE);
        cameraSwitchView.setVisibility(View.VISIBLE);
        settingsButton.setVisibility(GONE);

        if (AwesomeCamConfiguration.MEDIA_ACTION_UNSPECIFIED != mediaAction) {
            mediaActionSwitchView.setVisibility(GONE);
        } else mediaActionSwitchView.setVisibility(VISIBLE);
        recordButton.setRecordState(RecordButton.READY_FOR_RECORD_STATE);
    }

    @Override
    public void onStartRecordingButtonPressed() {

        cameraSwitchView.setVisibility(View.GONE);
        mediaActionSwitchView.setVisibility(GONE);
        settingsButton.setVisibility(GONE);

        if (recordButtonListener != null)
            recordButtonListener.onStartRecordingButtonPressed();
    }

    @Override
    public void onStopRecordingButtonPressed() {
        onStopVideoRecord();
        if (recordButtonListener != null)
            recordButtonListener.onStopRecordingButtonPressed();
    }

    @Override
    public void onMediaActionChanged(int mediaActionState) {
        setMediaActionState(mediaActionState);
        if (onMediaActionStateChangeListener != null)
            onMediaActionStateChangeListener.onMediaActionChanged(this.mediaActionState);
    }

    abstract static class TimerTaskBase {
        Handler handler = new Handler(Looper.getMainLooper());
        TextView timerView;
        boolean alive = false;
        long recordingTimeSeconds = 0;
        long recordingTimeMinutes = 0;

        TimerTaskBase(TextView timerView) {
            this.timerView = timerView;
        }

        abstract void stop();

        abstract void start();
    }

    private class CountdownTask extends TimerTaskBase implements Runnable {

        private int maxDurationMilliseconds;

        public CountdownTask(TextView timerView, int maxDurationMilliseconds) {
            super(timerView);
            this.maxDurationMilliseconds = maxDurationMilliseconds;
        }

        @Override
        public void run() {

            recordingTimeSeconds--;

            int millis = (int) recordingTimeSeconds * 1000;
            recordDuration = maxDurationMilliseconds - millis;
            recordDuration = (recordDuration < 0 ? 0 : recordDuration);

            timerView.setText(
                    String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(millis),
                            TimeUnit.MILLISECONDS.toSeconds(millis) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
                    ));

            if (recordingTimeSeconds < 5) {
                timerView.setTextColor(Color.RED);
            }

            if (alive && recordingTimeSeconds > 0) handler.postDelayed(this, DateTimeUtils.SECOND);
        }

        @Override
        void stop() {
            timerView.setVisibility(View.INVISIBLE);
            alive = false;
        }

        @Override
        void start() {
            alive = true;
            recordingTimeSeconds = maxDurationMilliseconds / 1000;
            timerView.setTextColor(Color.WHITE);
            timerView.setText(
                    String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(maxDurationMilliseconds),
                            TimeUnit.MILLISECONDS.toSeconds(maxDurationMilliseconds) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(maxDurationMilliseconds))
                    ));
            timerView.setVisibility(View.VISIBLE);
            handler.postDelayed(this, DateTimeUtils.SECOND);
        }
    }

    private class TimerTask extends TimerTaskBase implements Runnable {

        public TimerTask(TextView timerView) {
            super(timerView);
        }

        @Override
        public void run() {
            recordingTimeSeconds++;

            if (recordingTimeSeconds == 60) {
                recordingTimeSeconds = 0;
                recordingTimeMinutes++;
            }
            recordDuration = recordingTimeSeconds + recordingTimeMinutes * 60;
            timerView.setText(
                    String.format("%02d:%02d", recordingTimeMinutes, recordingTimeSeconds));
            if (alive) handler.postDelayed(this, DateTimeUtils.SECOND);
        }

        public void start() {
            alive = true;
            recordingTimeMinutes = 0;
            recordingTimeSeconds = 0;
            timerView.setText(
                    String.format("%02d:%02d", recordingTimeMinutes, recordingTimeSeconds));
            timerView.setVisibility(View.VISIBLE);
            handler.postDelayed(this, DateTimeUtils.SECOND);
        }

        public void stop() {
            timerView.setVisibility(View.INVISIBLE);
            alive = false;
        }
    }

}
