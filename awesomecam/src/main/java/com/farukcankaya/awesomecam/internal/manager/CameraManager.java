package com.farukcankaya.awesomecam.internal.manager;

import android.content.Context;

import java.io.File;

import com.farukcankaya.awesomecam.internal.configuration.AwesomeCamConfiguration;
import com.farukcankaya.awesomecam.internal.configuration.ConfigurationProvider;
import com.farukcankaya.awesomecam.internal.manager.impl.CameraHandler;
import com.farukcankaya.awesomecam.internal.manager.impl.ParametersHandler;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraCloseListener;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraOpenListener;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraPhotoListener;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraVideoListener;
import com.farukcankaya.awesomecam.internal.utils.Size;

/**
 * Created by memfis on 8/14/16.
 */
public interface CameraManager<CameraId, SurfaceListener, CameraParameters, Camera> {

    void initializeCameraManager(ConfigurationProvider configurationProvider, Context context);

    void openCamera(CameraId cameraId, CameraOpenListener<CameraId, SurfaceListener> cameraOpenListener);

    void closeCamera(CameraCloseListener<CameraId> cameraCloseListener);

    void setFlashMode(@AwesomeCamConfiguration.FlashMode int flashMode);

    void takePhoto(File photoFile, CameraPhotoListener cameraPhotoListener);

    void startVideoRecord(File videoFile, CameraVideoListener cameraVideoListener);

    Size getPhotoSizeForQuality(@AwesomeCamConfiguration.MediaQuality int mediaQuality);

    void stopVideoRecord();

    void releaseCameraManager();

    CameraId getCurrentCameraId();

    CameraId getFaceFrontCameraId();

    CameraId getFaceBackCameraId();

    int getNumberOfCameras();

    int getFaceFrontCameraOrientation();

    int getFaceBackCameraOrientation();

    boolean isVideoRecording();

    boolean handleParameters(ParametersHandler<CameraParameters> parameters);

    void handleCamera(CameraHandler<Camera> cameraHandler);
}
