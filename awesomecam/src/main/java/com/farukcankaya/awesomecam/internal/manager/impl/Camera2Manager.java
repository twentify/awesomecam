package com.farukcankaya.awesomecam.internal.manager.impl;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.anggrayudi.storage.media.MediaFile;
import com.farukcankaya.awesomecam.internal.configuration.AwesomeCamConfiguration;
import com.farukcankaya.awesomecam.internal.configuration.ConfigurationProvider;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraCloseListener;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraOpenListener;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraPhotoListener;
import com.farukcankaya.awesomecam.internal.manager.listener.CameraVideoListener;
import com.farukcankaya.awesomecam.internal.utils.CameraHelper;
import com.farukcankaya.awesomecam.internal.utils.ImageSaver;
import com.farukcankaya.awesomecam.internal.utils.Size;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Created by memfis on 8/9/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public final class Camera2Manager extends BaseCameraManager<String, TextureView.SurfaceTextureListener, CaptureRequest.Builder, CameraDevice>
        implements ImageReader.OnImageAvailableListener, TextureView.SurfaceTextureListener {

    private final static String TAG = "Camera2Manager";

    private static Camera2Manager currentInstance;

    private CameraOpenListener<String, TextureView.SurfaceTextureListener> cameraOpenListener;
    private CameraPhotoListener cameraPhotoListener;
    private CameraVideoListener cameraVideoListener;

    private MediaFile outputMediaFile;

    @CameraPreviewState
    private int previewState = STATE_PREVIEW;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRE_CAPTURE = 2;
    private static final int STATE_WAITING_NON_PRE_CAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;

    @IntDef({STATE_PREVIEW, STATE_WAITING_LOCK, STATE_WAITING_PRE_CAPTURE, STATE_WAITING_NON_PRE_CAPTURE, STATE_PICTURE_TAKEN})
    @Retention(RetentionPolicy.SOURCE)
    @interface CameraPreviewState {
    }

    private CameraManager manager;
    private CameraDevice cameraDevice;
    private CaptureRequest previewRequest;
    private CaptureRequest.Builder previewRequestBuilder;
    private CameraCaptureSession captureSession;

    private CameraCharacteristics frontCameraCharacteristics;
    private CameraCharacteristics backCameraCharacteristics;
    private StreamConfigurationMap frontCameraStreamConfigurationMap;
    private StreamConfigurationMap backCameraStreamConfigurationMap;

    private SurfaceTexture texture;

    private Surface workingSurface;
    private ImageReader imageReader;

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            currentInstance.cameraDevice = cameraDevice;
            if (cameraOpenListener != null) {
                uiHandler.post(() -> {
                    if (!TextUtils.isEmpty(currentCameraId) && previewSize != null && currentInstance != null)
                        cameraOpenListener.onCameraOpened(currentCameraId, previewSize, currentInstance);
                });
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            currentInstance.cameraDevice = null;

            uiHandler.post(() -> cameraOpenListener.onCameraOpenError());
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            currentInstance.cameraDevice = null;

            uiHandler.post(() -> cameraOpenListener.onCameraOpenError());
        }
    };

    private CameraCaptureSession.CaptureCallback captureCallback
            = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            processCaptureResult(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            processCaptureResult(result);
        }

    };

    private Camera2Manager() {
    }

    public static Camera2Manager getInstance() {
        if (currentInstance == null) currentInstance = new Camera2Manager();
        return currentInstance;
    }

    @Override
    public void initializeCameraManager(ConfigurationProvider configurationProvider, Context context) {
        super.initializeCameraManager(configurationProvider, context);

        this.manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        windowSize = new Size(size.x, size.y);

        try {
            String[] ids = manager.getCameraIdList();
            numberOfCameras = ids.length;
            for (String id : ids) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);

                int orientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (orientation == CameraCharacteristics.LENS_FACING_FRONT) {
                    faceFrontCameraId = id;
                    faceFrontCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    frontCameraCharacteristics = characteristics;
                } else {
                    faceBackCameraId = id;
                    faceBackCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    backCameraCharacteristics = characteristics;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during camera init");
        }
    }

    @Override
    public void openCamera(String cameraId, final CameraOpenListener<String, TextureView.SurfaceTextureListener> cameraOpenListener) {
        this.currentCameraId = cameraId;
        this.cameraOpenListener = cameraOpenListener;
        backgroundHandler.post(() -> {
            if (context == null || configurationProvider == null) {
                Log.e(TAG, "openCamera: ");
                if (cameraOpenListener != null) {
                    uiHandler.post(() -> cameraOpenListener.onCameraOpenError());
                }
                return;
            }
            prepareCameraOutputs();
            try {
                manager.openCamera(currentCameraId, stateCallback, backgroundHandler);
            } catch (Exception e) {
                Log.e(TAG, "openCamera: ", e);
                if (cameraOpenListener != null) {
                    uiHandler.post(() -> cameraOpenListener.onCameraOpenError());
                }
            }
        });
    }

    @Override
    public void closeCamera(final CameraCloseListener<String> cameraCloseListener) {
        backgroundHandler.post(() -> {
            closeCamera();
            if (cameraCloseListener != null) {
                uiHandler.post(() -> cameraCloseListener.onCameraClosed(currentCameraId));
            }
        });
    }

    @Override
    public void handleCamera(CameraHandler<CameraDevice> cameraHandler) {
        cameraHandler.handleCamera(cameraDevice);
    }

    @Override
    public boolean handleParameters(ParametersHandler<CaptureRequest.Builder> parameters) {
        try {
            previewRequestBuilder = parameters.getParameters(previewRequestBuilder);

            previewRequest = previewRequestBuilder.build();
            captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
            return true;
        } catch (Throwable ignore) {
        }
        return false;
    }

    @Override
    public void setFlashMode(@AwesomeCamConfiguration.FlashMode int flashMode) {
        setFlashModeAndBuildPreviewRequest(flashMode);
    }

    @Override
    public void takePhoto(MediaFile photoMediaFile, CameraPhotoListener cameraPhotoListener) {
        this.outputMediaFile = photoMediaFile;
        this.cameraPhotoListener = cameraPhotoListener;

        backgroundHandler.post(() -> lockFocus());

    }

    @Override
    public Size getPhotoSizeForQuality(@AwesomeCamConfiguration.MediaQuality int mediaQuality) {
        StreamConfigurationMap map = currentCameraId.equals(faceBackCameraId) ? backCameraStreamConfigurationMap : frontCameraStreamConfigurationMap;
        return CameraHelper.getPictureSize(Size.fromArray2(map.getOutputSizes(ImageFormat.JPEG)), mediaQuality);
    }

    @Override
    public void startVideoRecord(MediaFile videoMediaFile, final CameraVideoListener cameraVideoListener) {
        if (isVideoRecording || texture == null) return;

        this.outputMediaFile = videoMediaFile;
        this.cameraVideoListener = cameraVideoListener;

        if (cameraVideoListener != null)
            backgroundHandler.post(() -> {
                closePreviewSession();
                if (prepareVideoRecorder()) {

                    SurfaceTexture texture = currentInstance.texture;
                    texture.setDefaultBufferSize(videoSize.getWidth(), videoSize.getHeight());

                    try {
                        previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                        List<Surface> surfaces = new ArrayList<>();

                        Surface previewSurface = workingSurface;
                        surfaces.add(previewSurface);
                        previewRequestBuilder.addTarget(previewSurface);

                        workingSurface = videoRecorder.getSurface();
                        surfaces.add(workingSurface);
                        previewRequestBuilder.addTarget(workingSurface);

                        cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                captureSession = cameraCaptureSession;

                                previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                                try {
                                    captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                                } catch (Exception e) {
                                }

                                try {
                                    videoRecorder.start();
                                } catch (Exception ignore) {
                                    Log.e(TAG, "videoRecorder.start(): ", ignore);
                                }

                                isVideoRecording = true;

                                uiHandler.post(() -> cameraVideoListener.onVideoRecordStarted(videoSize));
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                                Log.d(TAG, "onConfigureFailed");
                            }
                        }, backgroundHandler);
                    } catch (Exception e) {
                        Log.e(TAG, "startVideoRecord: ", e);
                    }
                }
            });
    }

    @Override
    public void stopVideoRecord() {
        if (isVideoRecording)
            backgroundHandler.post(() -> {
                closePreviewSession();

                if (videoRecorder != null) {
                    try {
                        videoRecorder.stop();
                    } catch (Exception ignore) {
                    }
                }
                isVideoRecording = false;
                releaseVideoRecorder();

                if (cameraVideoListener != null) {
                    uiHandler.post(() -> cameraVideoListener.onVideoRecordStopped(outputMediaFile));
                }
            });
    }

    //--------------------Internal methods------------------

    private void startPreview(SurfaceTexture texture) {
        try {
            if (texture == null) return;

            this.texture = texture;

            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            workingSurface = new Surface(texture);

            previewRequestBuilder
                    = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(workingSurface);

            cameraDevice.createCaptureSession(Arrays.asList(workingSurface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            updatePreview(cameraCaptureSession);
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.d(TAG, "Fail while starting preview: ");
                        }
                    }, null);
        } catch (Exception e) {
            Log.e(TAG, "Error while preparing surface for preview: ", e);
        }
    }

    @Override
    protected void onMaxDurationReached() {
        stopVideoRecord();
    }

    @Override
    protected void onMaxFileSizeReached() {
        stopVideoRecord();
    }

    @Override
    protected int getPhotoOrientation(@AwesomeCamConfiguration.SensorPosition int sensorPosition) {
        return getVideoOrientation(sensorPosition);
    }

    @Override
    protected int getVideoOrientation(@AwesomeCamConfiguration.SensorPosition int sensorPosition) {
        int degrees = 0;
        switch (sensorPosition) {
            case AwesomeCamConfiguration.SENSOR_POSITION_UP:
                degrees = 0;
                break; // Natural orientation
            case AwesomeCamConfiguration.SENSOR_POSITION_LEFT:
                degrees = 90;
                break; // Landscape left
            case AwesomeCamConfiguration.SENSOR_POSITION_UP_SIDE_DOWN:
                degrees = 180;
                break;// Upside down
            case AwesomeCamConfiguration.SENSOR_POSITION_RIGHT:
                degrees = 270;
                break;// Landscape right
            case AwesomeCamConfiguration.SENSOR_POSITION_UNSPECIFIED:
                break;
        }

        int rotate;
        if (Objects.equals(currentCameraId, faceFrontCameraId)) {
            rotate = (360 + faceFrontCameraOrientation + degrees) % 360;
        } else {
            rotate = (360 + faceBackCameraOrientation - degrees) % 360;
        }
        return rotate;
    }

    private void closeCamera() {
        closePreviewSession();
        releaseTexture();
        closeCameraDevice();
        closeImageReader();
        releaseVideoRecorder();
    }

    private void releaseTexture() {
        if (null != texture) {
            texture.release();
            texture = null;
        }
    }

    private void closeImageReader() {
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    private void closeCameraDevice() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    @Override
    protected void prepareCameraOutputs() {
        try {
            CameraCharacteristics characteristics = currentCameraId.equals(faceBackCameraId) ? backCameraCharacteristics : frontCameraCharacteristics;

            if (currentCameraId.equals(faceFrontCameraId) && frontCameraStreamConfigurationMap == null)
                frontCameraStreamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            else if (currentCameraId.equals(faceBackCameraId) && backCameraStreamConfigurationMap == null)
                backCameraStreamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            StreamConfigurationMap map = currentCameraId.equals(faceBackCameraId) ? backCameraStreamConfigurationMap : frontCameraStreamConfigurationMap;
            if (configurationProvider.getMediaQuality() == AwesomeCamConfiguration.MEDIA_QUALITY_AUTO) {
                camcorderProfile = CameraHelper.getCamcorderProfile(currentCameraId, configurationProvider.getVideoFileSize(), configurationProvider.getMinimumVideoDuration());
            } else
                camcorderProfile = CameraHelper.getCamcorderProfile(configurationProvider.getMediaQuality(), currentCameraId);

            videoSize = CameraHelper.chooseOptimalSize(Size.fromArray2(map.getOutputSizes(MediaRecorder.class)),
                    windowSize.getWidth(), windowSize.getHeight(), new Size(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight));

            if (videoSize == null || videoSize.getWidth() > camcorderProfile.videoFrameWidth
                    || videoSize.getHeight() > camcorderProfile.videoFrameHeight)
                videoSize = CameraHelper.getSizeWithClosestRatio(Size.fromArray2(map.getOutputSizes(MediaRecorder.class)), camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);

            photoSize = CameraHelper.getPictureSize(Size.fromArray2(map.getOutputSizes(ImageFormat.JPEG)),
                    configurationProvider.getMediaQuality() == AwesomeCamConfiguration.MEDIA_QUALITY_AUTO
                            ? AwesomeCamConfiguration.MEDIA_QUALITY_HIGHEST : configurationProvider.getMediaQuality());

            imageReader = ImageReader.newInstance(photoSize.getWidth(), photoSize.getHeight(),
                    ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(this, backgroundHandler);

            if (configurationProvider.getMediaAction() == AwesomeCamConfiguration.MEDIA_ACTION_PHOTO
                    || configurationProvider.getMediaAction() == AwesomeCamConfiguration.MEDIA_ACTION_UNSPECIFIED) {

                if (windowSize.getHeight() * windowSize.getWidth() > photoSize.getWidth() * photoSize.getHeight()) {
                    previewSize = CameraHelper.getOptimalPreviewSize(Size.fromArray2(map.getOutputSizes(SurfaceTexture.class)), photoSize.getWidth(), photoSize.getHeight());
                } else {
                    previewSize = CameraHelper.getOptimalPreviewSize(Size.fromArray2(map.getOutputSizes(SurfaceTexture.class)), windowSize.getWidth(), windowSize.getHeight());
                }

                if (previewSize == null)
                    previewSize = CameraHelper.chooseOptimalSize(Size.fromArray2(map.getOutputSizes(SurfaceTexture.class)), windowSize.getWidth(), windowSize.getHeight(), photoSize);

            } else {
                if (windowSize.getHeight() * windowSize.getWidth() > videoSize.getWidth() * videoSize.getHeight()) {
                    previewSize = CameraHelper.getOptimalPreviewSize(Size.fromArray2(map.getOutputSizes(SurfaceTexture.class)), videoSize.getWidth(), videoSize.getHeight());
                } else {
                    previewSize = CameraHelper.getOptimalPreviewSize(Size.fromArray2(map.getOutputSizes(SurfaceTexture.class)), windowSize.getWidth(), windowSize.getHeight());
                }

                if (previewSize == null)
                    previewSize = CameraHelper.getSizeWithClosestRatio(Size.fromArray2(map.getOutputSizes(SurfaceTexture.class)), videoSize.getWidth(), videoSize.getHeight());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while setup camera sizes.", e);
        }
    }

    @Override
    protected boolean prepareVideoRecorder() {
        videoRecorder = new MediaRecorder();
        try {
            videoRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            videoRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);

            videoRecorder.setOutputFormat(camcorderProfile.fileFormat);
            videoRecorder.setVideoFrameRate(camcorderProfile.videoFrameRate);
            videoRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
            videoRecorder.setVideoEncodingBitRate(camcorderProfile.videoBitRate);
            videoRecorder.setVideoEncoder(camcorderProfile.videoCodec);

            videoRecorder.setAudioEncodingBitRate(camcorderProfile.audioBitRate);
            videoRecorder.setAudioChannels(camcorderProfile.audioChannels);
            videoRecorder.setAudioSamplingRate(camcorderProfile.audioSampleRate);
            videoRecorder.setAudioEncoder(camcorderProfile.audioCodec);

            try {
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(outputMediaFile.getUri(), "rw");
                if(pfd != null) {
                    videoRecorder.setOutputFile(pfd.getFileDescriptor());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if (configurationProvider.getVideoFileSize() > 0) {
                videoRecorder.setMaxFileSize(configurationProvider.getVideoFileSize());
                videoRecorder.setOnInfoListener(this);
            }
            if (configurationProvider.getVideoDuration() > 0) {
                videoRecorder.setMaxDuration(configurationProvider.getVideoDuration());
                videoRecorder.setOnInfoListener(this);
            }
            videoRecorder.setOrientationHint(getVideoOrientation(configurationProvider.getSensorPosition()));

            videoRecorder.prepare();

            return true;
        } catch (IllegalStateException error) {
            Log.e(TAG, "IllegalStateException preparing MediaRecorder: " + error.getMessage());
        } catch (IOException error) {
            Log.e(TAG, "IOException preparing MediaRecorder: " + error.getMessage());
        } catch (Throwable error) {
            Log.e(TAG, "Error during preparing MediaRecorder: " + error.getMessage());
        }

        releaseVideoRecorder();
        return false;
    }

    private void updatePreview(CameraCaptureSession cameraCaptureSession) {
        if (null == cameraDevice) {
            return;
        }
        captureSession = cameraCaptureSession;
        setFlashModeAndBuildPreviewRequest(configurationProvider.getFlashMode());

        if (cameraOpenListener != null) cameraOpenListener.onCameraReady();
    }

    private void closePreviewSession() {
        if (captureSession != null) {
            captureSession.close();
            try {
                captureSession.abortCaptures();
            } catch (Exception ignore) {
            } finally {
                captureSession = null;
            }
        }
    }

    private void lockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);

            previewState = STATE_WAITING_LOCK;
            captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
        } catch (Exception ignore) {
        }
    }

    private void processCaptureResult(CaptureResult result) {
        switch (previewState) {
            case STATE_PREVIEW: {
                break;
            }
            case STATE_WAITING_LOCK: {
                Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                if (afState == null) {
                    captureStillPicture();
                } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState
                        || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
                        || CaptureResult.CONTROL_AF_STATE_INACTIVE == afState
                        || CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN == afState) {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        previewState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    } else {
                        runPreCaptureSequence();
                    }
                }
                break;
            }
            case STATE_WAITING_PRE_CAPTURE: {
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                    previewState = STATE_WAITING_NON_PRE_CAPTURE;
                }
                break;
            }
            case STATE_WAITING_NON_PRE_CAPTURE: {
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    previewState = STATE_PICTURE_TAKEN;
                    captureStillPicture();
                }
                break;
            }
            case STATE_PICTURE_TAKEN:
                break;
        }
    }

    private void runPreCaptureSequence() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            previewState = STATE_WAITING_PRE_CAPTURE;
            captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
        } catch (CameraAccessException e) {
        }
    }

    private void captureStillPicture() {
        try {
            if (null == cameraDevice) {
                return;
            }
            final CaptureRequest.Builder captureBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getPhotoOrientation(configurationProvider.getSensorPosition()));

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    Log.d(TAG, "onCaptureCompleted: ");
                }
            };

            captureSession.stopRepeating();
            captureSession.capture(captureBuilder.build(), CaptureCallback, null);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Error during capturing picture");
        }
    }

    private void unlockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
            previewState = STATE_PREVIEW;
            captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "Error during focus unlocking");
        }
    }

    private void setFlashModeAndBuildPreviewRequest(@AwesomeCamConfiguration.FlashMode int flashMode) {
        try {

            switch (flashMode) {
                case AwesomeCamConfiguration.FLASH_MODE_ON:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                    break;
                case AwesomeCamConfiguration.FLASH_MODE_OFF:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    break;
                case AwesomeCamConfiguration.FLASH_MODE_AUTO:
                default:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                    break;
            }

            previewRequest = previewRequestBuilder.build();

            try {
                captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
            } catch (Exception e) {
                Log.e(TAG, "Error updating preview: ", e);
            }
        } catch (Exception ignore) {
            Log.e(TAG, "Error setting flash: ", ignore);
        }
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        MediaFile outputFile = outputMediaFile;
        backgroundHandler.post(new ImageSaver(imageReader.acquireNextImage(), outputFile, new ImageSaver.ImageSaverCallback() {
            @Override
            public void onSuccessFinish() {
                Log.d(TAG, "onPhotoSuccessFinish: ");
                if (cameraPhotoListener != null) {
                    uiHandler.post(() -> cameraPhotoListener.onPhotoTaken(outputMediaFile));
                }
                unlockFocus();
            }

            @Override
            public void onError() {
                Log.d(TAG, "onPhotoError: ");
                uiHandler.post(() -> cameraPhotoListener.onPhotoTakeError());
            }
        }));
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (surfaceTexture != null) startPreview(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        if (surfaceTexture != null) startPreview(surfaceTexture);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

}
