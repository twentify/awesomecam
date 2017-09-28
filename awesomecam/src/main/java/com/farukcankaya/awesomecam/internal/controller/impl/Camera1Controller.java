package com.farukcankaya.awesomecam.internal.controller.impl;

import android.hardware.Camera;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.File;

import com.farukcankaya.awesomecam.internal.configuration.AwesomeCamConfiguration;
import com.farukcankaya.awesomecam.internal.configuration.ConfigurationProvider;
import com.farukcankaya.awesomecam.internal.controller.view.CameraView;
import com.farukcankaya.awesomecam.internal.manager.CameraManager;
import com.farukcankaya.awesomecam.internal.manager.impl.Camera1Manager;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraCloseListener;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraOpenListener;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraPhotoListener;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraVideoListener;
import com.farukcankaya.awesomecam.internal.ui.view.AutoFitSurfaceView;
import com.farukcankaya.awesomecam.internal.ui.view.CameraSwitchView;
import com.farukcankaya.awesomecam.internal.utils.CameraHelper;
import com.farukcankaya.awesomecam.internal.utils.Size;

/**
 * Created by memfis on 7/7/16.
 */

@SuppressWarnings("deprecation")
public class Camera1Controller implements com.farukcankaya.awesomecam.internal.controller.CameraController<Integer>,
        CameraOpenListener<Integer, SurfaceHolder.Callback>, CameraPhotoListener, CameraCloseListener<Integer>, CameraVideoListener {

    private final static String TAG = "Camera1Controller";

    private Integer currentCameraId;
    private ConfigurationProvider configurationProvider;
    private CameraManager<Integer, SurfaceHolder.Callback, Camera.Parameters, Camera> cameraManager;
    private CameraView cameraView;

    private File outputFile;

    public Camera1Controller(CameraView cameraView, ConfigurationProvider configurationProvider) {
        this.cameraView = cameraView;
        this.configurationProvider = configurationProvider;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        cameraManager = Camera1Manager.getInstance();
        cameraManager.initializeCameraManager(configurationProvider, cameraView.getActivity());

        if (configurationProvider.getCameraFace() == CameraSwitchView.CAMERA_TYPE_FRONT) {
            currentCameraId = cameraManager.getFaceFrontCameraId() == null ? cameraManager.getFaceBackCameraId() : cameraManager.getFaceFrontCameraId();
        } else {
            currentCameraId = cameraManager.getFaceBackCameraId();
        }
    }

    @Override
    public void openCamera() {
        cameraManager.openCamera(currentCameraId, this);
    }

    @Override
    public void onResume() {
        openCamera();
    }

    @Override
    public void onPause() {
        cameraManager.closeCamera(null);
    }

    @Override
    public void onDestroy() {
        cameraManager.releaseCameraManager();
    }

    @Override
    public void takePhoto() {
        outputFile = TextUtils.isEmpty(configurationProvider.getFilePath()) ? CameraHelper.getOutputMediaFile(cameraView.getActivity(), AwesomeCamConfiguration.MEDIA_ACTION_PHOTO) : new File(configurationProvider.getFilePath());
        cameraManager.takePhoto(outputFile, this);
    }

    @Override
    public void startVideoRecord() {
        outputFile = TextUtils.isEmpty(configurationProvider.getFilePath()) ? CameraHelper.getOutputMediaFile(cameraView.getActivity(), AwesomeCamConfiguration.MEDIA_ACTION_VIDEO) : new File(configurationProvider.getFilePath());
        cameraManager.startVideoRecord(outputFile, this);
    }

    @Override
    public void stopVideoRecord() {
        cameraManager.stopVideoRecord();
    }

    @Override
    public boolean isVideoRecording() {
        return cameraManager.isVideoRecording();
    }

    @Override
    public void switchCamera(@AwesomeCamConfiguration.CameraFace final int cameraFace) {
        currentCameraId = cameraManager.getCurrentCameraId().equals(cameraManager.getFaceFrontCameraId()) ?
                cameraManager.getFaceBackCameraId() : cameraManager.getFaceFrontCameraId();

        cameraManager.closeCamera(this);
    }

    @Override
    public void setFlashMode(@AwesomeCamConfiguration.FlashMode int flashMode) {
        cameraManager.setFlashMode(flashMode);
    }

    @Override
    public void switchQuality() {
        cameraManager.closeCamera(this);
    }

    @Override
    public int getNumberOfCameras() {
        return cameraManager.getNumberOfCameras();
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
    public Integer getCurrentCameraId() {
        return currentCameraId;
    }


    @Override
    public void onCameraOpened(Integer cameraId, Size previewSize, SurfaceHolder.Callback surfaceCallback) {
        cameraView.updateUiForMediaAction(configurationProvider.getMediaAction());
        cameraView.updateCameraPreview(previewSize, new AutoFitSurfaceView(cameraView.getActivity(), surfaceCallback));
        cameraView.updateCameraSwitcher(getNumberOfCameras());
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
    public void onCameraClosed(Integer closedCameraId) {
        cameraView.releaseCameraPreview();

        cameraManager.openCamera(currentCameraId, this);
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
        return cameraManager;
    }
}
