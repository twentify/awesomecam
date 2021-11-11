package com.farukcankaya.awesomecam.internal.configuration;

import android.net.Uri;

import com.farukcankaya.awesomecam.internal.ui.view.CameraSwitchView;

/**
 * Created by memfis on 7/6/16.
 */
public interface ConfigurationProvider {

    int getRequestCode();

    @AwesomeCamConfiguration.MediaAction
    int getMediaAction();

    @AwesomeCamConfiguration.MediaQuality
    int getMediaQuality();

    int getVideoDuration();

    long getVideoFileSize();

    @AwesomeCamConfiguration.SensorPosition
    int getSensorPosition();

    int getDegrees();

    int getMinimumVideoDuration();

    @AwesomeCamConfiguration.FlashMode
    int getFlashMode();

    @CameraSwitchView.CameraType
    int getCameraFace();

    Uri getFileUri();

    @AwesomeCamConfiguration.MediaResultBehaviour
    int getMediaResultBehaviour();
}
