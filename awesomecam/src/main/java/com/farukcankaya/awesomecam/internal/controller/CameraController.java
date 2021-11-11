package com.farukcankaya.awesomecam.internal.controller;

import android.os.Bundle;

import com.anggrayudi.storage.media.MediaFile;
import com.farukcankaya.awesomecam.internal.configuration.AwesomeCamConfiguration;
import com.farukcankaya.awesomecam.internal.manager.CameraManager;

/**
 * Created by memfis on 7/6/16.
 */
public interface CameraController<CameraId> {

    void onCreate(Bundle savedInstanceState);

    void onResume();

    void onPause();

    void onDestroy();

    void takePhoto();

    void startVideoRecord();

    void stopVideoRecord();

    boolean isVideoRecording();

    void openCamera();

    void switchCamera(@AwesomeCamConfiguration.CameraFace int cameraFace);

    void switchQuality();

    void setFlashMode(@AwesomeCamConfiguration.FlashMode int flashMode);

    int getNumberOfCameras();

    @AwesomeCamConfiguration.MediaAction
    int getMediaAction();

    CameraId getCurrentCameraId();

    MediaFile getOutputMediaFile();

    CameraManager getCameraManager();
}
