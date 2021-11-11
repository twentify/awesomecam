package com.farukcankaya.awesomecam.internal.ui.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaActionSound;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import com.farukcankaya.awesomecam.R;
import com.farukcankaya.awesomecam.internal.configuration.AwesomeCamConfiguration;
import com.farukcankaya.awesomecam.internal.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by memfis on 7/6/16.
 */
public class RecordButton extends AppCompatImageButton {

    public static final int TAKE_PHOTO_STATE = 0;
    public static final int READY_FOR_RECORD_STATE = 1;
    public static final int RECORD_IN_PROGRESS_STATE = 2;

    @IntDef({TAKE_PHOTO_STATE, READY_FOR_RECORD_STATE, RECORD_IN_PROGRESS_STATE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RecordState {
    }

    public interface RecordButtonListener {

        void onTakePhotoButtonPressed();

        void onStartRecordingButtonPressed();

        void onStopRecordingButtonPressed();
    }

    public interface MinimumRecordCallback {
        boolean isDurationEnoughToStopRecord();
    }

    private Context context;
    private int mediaAction = AwesomeCamConfiguration.MEDIA_ACTION_PHOTO;

    private
    @RecordState
    int currentState = TAKE_PHOTO_STATE;

    private Drawable takePhotoDrawable;
    private Drawable startRecordDrawable;
    private Drawable stopRecordDrawable;
    private int iconPadding = 8;

    private RecordButtonListener listener;
    private MinimumRecordCallback minimumRecordCallback;

    public RecordButton(@NonNull Context context) {
        this(context, null, 0);
    }

    public RecordButton(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordButton(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        takePhotoDrawable = ContextCompat.getDrawable(context, R.drawable.take_photo_button);
        startRecordDrawable = ContextCompat.getDrawable(context, R.drawable.start_video_record_button);
        stopRecordDrawable = ContextCompat.getDrawable(context, R.drawable.stop_button_background);
    }

    public void setup(@AwesomeCamConfiguration.MediaAction int mediaAction, @NonNull RecordButtonListener listener, @Nullable MinimumRecordCallback minimumRecordCallback) {
        setMediaAction(mediaAction);
        this.listener = listener;
        this.minimumRecordCallback = minimumRecordCallback;

//        setBackground(ContextCompat.getDrawable(context, R.drawable.circle_frame_background_dark));
        setBackground(ContextCompat.getDrawable(context, R.drawable.circle_frame_background));

        setIcon();
        setOnClickListener(new RecordClickListener());
        setSoundEffectsEnabled(false);
        setIconPadding(iconPadding);
    }

    private void setIconPadding(int paddingDP) {
        int padding = Utils.convertDipToPixels(context, paddingDP);
        setPadding(padding, padding, padding, padding);
    }

    public void setMediaAction(@AwesomeCamConfiguration.MediaAction int mediaAction) {
        this.mediaAction = mediaAction;
        if (AwesomeCamConfiguration.MEDIA_ACTION_PHOTO == mediaAction)
            currentState = TAKE_PHOTO_STATE;
        else currentState = READY_FOR_RECORD_STATE;
        setRecordState(currentState);
        setIcon();
    }

    public void setRecordState(@RecordState int state) {
        currentState = state;
        setIcon();
    }

    public
    @RecordState
    int getRecordState() {
        return currentState;
    }

    public void setRecordButtonListener(@NonNull RecordButtonListener listener) {
        this.listener = listener;
    }

    private void setIcon() {
        if (AwesomeCamConfiguration.MEDIA_ACTION_VIDEO == mediaAction) {
            if (READY_FOR_RECORD_STATE == currentState) {
                setImageDrawable(startRecordDrawable);
                setIconPadding(iconPadding);
            } else if (RECORD_IN_PROGRESS_STATE == currentState) {
                setImageDrawable(stopRecordDrawable);
                int iconPaddingStop = 18;
                setIconPadding(iconPaddingStop);
            }
        } else {
            setImageDrawable(takePhotoDrawable);
            setIconPadding(iconPadding);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void takePhoto(MediaActionSound sound) {
        sound.play(MediaActionSound.SHUTTER_CLICK);
        takePhoto();
    }

    private void takePhoto() {
        if (listener != null)
            listener.onTakePhotoButtonPressed();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startRecording(MediaActionSound sound) {
        sound.play(MediaActionSound.START_VIDEO_RECORDING);
        startRecording();
    }

    private void startRecording() {
        currentState = RECORD_IN_PROGRESS_STATE;
        if (listener != null) {
            listener.onStartRecordingButtonPressed();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void stopRecording(MediaActionSound sound) {
        sound.play(MediaActionSound.STOP_VIDEO_RECORDING);
        stopRecording();
    }

    private void stopRecording() {
        currentState = READY_FOR_RECORD_STATE;
        if (listener != null) {
            listener.onStopRecordingButtonPressed();
        }
    }

    private class RecordClickListener implements OnClickListener {

        private final static int CLICK_DELAY = 1000;

        private long lastClickTime = 0;

        @Override
        public void onClick(View view) {
            if (System.currentTimeMillis() - lastClickTime < CLICK_DELAY) {
                return;
            } else lastClickTime = System.currentTimeMillis();

            MediaActionSound sound = new MediaActionSound();
            if (TAKE_PHOTO_STATE == currentState) {
                takePhoto(sound);
            } else if (READY_FOR_RECORD_STATE == currentState) {
                startRecording(sound);
            } else if (RECORD_IN_PROGRESS_STATE == currentState) {
                if (minimumRecordCallback == null || minimumRecordCallback.isDurationEnoughToStopRecord()) {
                    stopRecording(sound);
                }
            }
            setIcon();
        }
    }

}
