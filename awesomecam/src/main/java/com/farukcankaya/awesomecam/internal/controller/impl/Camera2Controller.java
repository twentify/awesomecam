package com.farukcankaya.awesomecam.internal.controller.impl;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;

import java.io.File;

import com.farukcankaya.awesomecam.internal.configuration.AwesomeCamConfiguration;
import com.farukcankaya.awesomecam.internal.configuration.ConfigurationProvider;
import com.farukcankaya.awesomecam.internal.controller.CameraController;
import com.farukcankaya.awesomecam.internal.controller.view.CameraView;
import com.farukcankaya.awesomecam.internal.manager.CameraManager;
import com.farukcankaya.awesomecam.internal.manager.impl.Camera2Manager;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraCloseListener;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraOpenListener;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraPhotoListener;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraVideoListener;
import com.farukcankaya.awesomecam.internal.ui.view.AutoFitTextureView;
import com.farukcankaya.awesomecam.internal.ui.view.CameraSwitchView;
import com.farukcankaya.awesomecam.internal.utils.CameraHelper;
import com.farukcankaya.awesomecam.internal.utils.Size;

/**
 * Created by memfis on 7/6/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Controller implements CameraController<String>,
        CameraOpenListener<String, TextureView.SurfaceTextureListener>,
        CameraPhotoListener, CameraVideoListener, CameraCloseListener<String> {

    private final static String TAG = "Camera2Controller";

    private String currentCameraId;
    private ConfigurationProvider configurationProvider;
    private CameraManager<String, TextureView.SurfaceTextureListener, CaptureRequest.Builder, CameraDevice> camera2Manager;
    private CameraView cameraView;

    private File outputFile;

    public Camera2Controller(CameraView cameraView, ConfigurationProvider configurationProvider) {
        this.cameraView = cameraView;
        this.configurationProvider = configurationProvider;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        camera2Manager = Camera2Manager.getInstance();
        camera2Manager.initializeCameraManager(configurationProvider, cameraView.getActivity());

        if (configurationProvider.getCameraFace() == CameraSwitchView.CAMERA_TYPE_FRONT) {
            currentCameraId = camera2Manager.getFaceFrontCameraId() == null ? camera2Manager.getFaceBackCameraId() : camera2Manager.getFaceFrontCameraId();
        } else {
            currentCameraId = camera2Manager.getFaceBackCameraId();
        }
    }

    @Override
    public void openCamera() {
        camera2Manager.closeCamera(this);
    }

    @Override
    public void onResume() {
        openCamera();
    }

    @Override
    public void onPause() {
        camera2Manager.closeCamera(null);
        cameraView.releaseCameraPreview();
    }

    @Override
    public void onDestroy() {
        camera2Manager.releaseCameraManager();
    }

    @Override
    public void takePhoto() {
        outputFile = TextUtils.isEmpty(configurationProvider.getFilePath()) ? CameraHelper.getOutputMediaFile(cameraView.getActivity(), AwesomeCamConfiguration.MEDIA_ACTION_PHOTO) : new File(configurationProvider.getFilePath());
        camera2Manager.takePhoto(outputFile, this);
    }

    @Override
    public void startVideoRecord() {
        outputFile = TextUtils.isEmpty(configurationProvider.getFilePath()) ? CameraHelper.getOutputMediaFile(cameraView.getActivity(), AwesomeCamConfiguration.MEDIA_ACTION_VIDEO) : new File(configurationProvider.getFilePath());
        camera2Manager.startVideoRecord(outputFile, this);
    }

    @Override
    public void stopVideoRecord() {
        camera2Manager.stopVideoRecord();
    }

    @Override
    public boolean isVideoRecording() {
        return camera2Manager.isVideoRecording();
    }

    @Override
    public void switchCamera(final @AwesomeCamConfiguration.CameraFace int cameraFace) {
        currentCameraId = camera2Manager.getCurrentCameraId().equals(camera2Manager.getFaceFrontCameraId()) ?
                camera2Manager.getFaceBackCameraId() : camera2Manager.getFaceFrontCameraId();

        camera2Manager.closeCamera(this);
    }

    @Override
    public void setFlashMode(@AwesomeCamConfiguration.FlashMode int flashMode) {
        camera2Manager.setFlashMode(flashMode);
    }

    @Override
    public void switchQuality() {
        camera2Manager.closeCamera(this);
    }

    @Override
    public int getNumberOfCameras() {
        return camera2Manager.getNumberOfCameras();
    }

    @Override
    public int getMediaAction() {
        return configurationProvider.getMediaAction();
    }

    @Override
    public File getOutputFile() {
        return outputFile;
    }

    @Override
    public String getCurrentCameraId() {
        return currentCameraId;
    }

    @Override
    public void onCameraOpened(String openedCameraId, Size previewSize, TextureView.SurfaceTextureListener surfaceTextureListener) {
        cameraView.updateUiForMediaAction(AwesomeCamConfiguration.MEDIA_ACTION_UNSPECIFIED);
        cameraView.updateCameraPreview(previewSize, new AutoFitTextureView(cameraView.getActivity(), surfaceTextureListener));
        cameraView.updateCameraSwitcher(camera2Manager.getNumberOfCameras());
    }

    @Override
    public void onCameraReady() {
        cameraView.onCameraReady();
    }

    @Override
    public void onCameraOpenError() {
        Log.e(TAG, "onCameraOpenError");
    }

    @Override
    public void onCameraClosed(String closedCameraId) {
        cameraView.releaseCameraPreview();

        camera2Manager.openCamera(currentCameraId, this);
    }

    @Override
    public void onPhotoTaken(File photoFile) {
        cameraView.onPhotoTaken();
    }

    @Override
    public void onPhotoTakeError() {
    }

    @Override
    public void onVideoRecordStarted(Size videoSize) {
        cameraView.onVideoRecordStart(videoSize.getWidth(), videoSize.getHeight());
    }

    @Override
    public void onVideoRecordStopped(File videoFile) {
        cameraView.onVideoRecordStop();
    }

    @Override
    public void onVideoRecordError() {

    }

    @Override
    public CameraManager getCameraManager() {
        return camera2Manager;
    }
}
