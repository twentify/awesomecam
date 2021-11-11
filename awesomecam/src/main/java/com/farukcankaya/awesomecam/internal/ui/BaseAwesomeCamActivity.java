package com.farukcankaya.awesomecam.internal.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.farukcankaya.awesomecam.R;
import com.farukcankaya.awesomecam.internal.configuration.AwesomeCamConfiguration;
import com.farukcankaya.awesomecam.internal.ui.model.PhotoQualityOption;
import com.farukcankaya.awesomecam.internal.ui.model.VideoQualityOption;
import com.farukcankaya.awesomecam.internal.ui.preview.PreviewActivity;
import com.farukcankaya.awesomecam.internal.ui.view.CameraControlPanel;
import com.farukcankaya.awesomecam.internal.ui.view.CameraSwitchView;
import com.farukcankaya.awesomecam.internal.ui.view.FlashSwitchView;
import com.farukcankaya.awesomecam.internal.ui.view.MediaActionSwitchView;
import com.farukcankaya.awesomecam.internal.ui.view.RecordButton;
import com.farukcankaya.awesomecam.internal.utils.Size;
import com.farukcankaya.awesomecam.internal.utils.Utils;

/**
 * Created by memfis on 12/1/16.
 */

public abstract class BaseAwesomeCamActivity<CameraId> extends AwesomeCamCameraActivity<CameraId>
        implements
        RecordButton.RecordButtonListener,
        RecordButton.MinimumRecordCallback,
        FlashSwitchView.FlashModeSwitchListener,
        MediaActionSwitchView.OnMediaActionStateChangeListener,
        CameraSwitchView.OnCameraTypeChangeListener, CameraControlPanel.SettingsClickListener {

    private CameraControlPanel cameraControlPanel;
    private AlertDialog settingsDialog;

    protected static final int REQUEST_PREVIEW_CODE = 1001;

    public static final int ACTION_CONFIRM = 900;
    public static final int ACTION_RETAKE = 901;
    public static final int ACTION_CANCEL = 902;

    protected int requestCode = -1;

    @AwesomeCamConfiguration.MediaAction
    protected int mediaAction = AwesomeCamConfiguration.MEDIA_ACTION_UNSPECIFIED;
    @AwesomeCamConfiguration.MediaQuality
    protected int mediaQuality = AwesomeCamConfiguration.MEDIA_QUALITY_MEDIUM;
    @AwesomeCamConfiguration.MediaQuality
    protected int passedMediaQuality = AwesomeCamConfiguration.MEDIA_QUALITY_MEDIUM;

    @AwesomeCamConfiguration.FlashMode
    protected int flashMode = AwesomeCamConfiguration.FLASH_MODE_AUTO;

    protected CharSequence[] videoQualities;
    protected CharSequence[] photoQualities;

    protected int videoDuration = -1;
    protected long videoFileSize = -1;
    protected int minimumVideoDuration = -1;
    protected Uri fileUri;

    @MediaActionSwitchView.MediaActionState
    protected int currentMediaActionState;

    @CameraSwitchView.CameraType
    protected int currentCameraType = CameraSwitchView.CAMERA_TYPE_REAR;

    @AwesomeCamConfiguration.MediaResultBehaviour
    private int mediaResultBehaviour = AwesomeCamConfiguration.PREVIEW;

    @AwesomeCamConfiguration.MediaQuality
    protected int newQuality = AwesomeCamConfiguration.MEDIA_QUALITY_AUTO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onProcessBundle(Bundle savedInstanceState) {
        super.onProcessBundle(savedInstanceState);

        extractConfiguration(getIntent().getExtras());
        currentMediaActionState = mediaAction == AwesomeCamConfiguration.MEDIA_ACTION_VIDEO ?
                MediaActionSwitchView.ACTION_VIDEO : MediaActionSwitchView.ACTION_PHOTO;
    }

    @Override
    protected void onCameraControllerReady() {
        super.onCameraControllerReady();

        videoQualities = getVideoQualityOptions();
        photoQualities = getPhotoQualityOptions();
    }

    @Override
    protected void onResume() {
        super.onResume();

        cameraControlPanel.lockControls();
        cameraControlPanel.allowRecord(false);
    }

    @Override
    protected void onPause() {
        super.onPause();

        cameraControlPanel.lockControls();
        cameraControlPanel.allowRecord(false);
    }

    private void extractConfiguration(Bundle bundle) {
        if (bundle != null) {
            if (bundle.containsKey(AwesomeCamConfiguration.Arguments.REQUEST_CODE))
                requestCode = bundle.getInt(AwesomeCamConfiguration.Arguments.REQUEST_CODE);

            if (bundle.containsKey(AwesomeCamConfiguration.Arguments.MEDIA_ACTION)) {
                switch (bundle.getInt(AwesomeCamConfiguration.Arguments.MEDIA_ACTION)) {
                    case AwesomeCamConfiguration.MEDIA_ACTION_PHOTO:
                        mediaAction = AwesomeCamConfiguration.MEDIA_ACTION_PHOTO;
                        break;
                    case AwesomeCamConfiguration.MEDIA_ACTION_VIDEO:
                        mediaAction = AwesomeCamConfiguration.MEDIA_ACTION_VIDEO;
                        break;
                    default:
                        mediaAction = AwesomeCamConfiguration.MEDIA_ACTION_UNSPECIFIED;
                        break;
                }
            }

            if (bundle.containsKey(AwesomeCamConfiguration.Arguments.MEDIA_RESULT_BEHAVIOUR)) {
                switch (bundle.getInt(AwesomeCamConfiguration.Arguments.MEDIA_RESULT_BEHAVIOUR)) {
                    case AwesomeCamConfiguration.CLOSE:
                        mediaResultBehaviour = AwesomeCamConfiguration.CLOSE;
                        break;
                    case AwesomeCamConfiguration.CONTINUE:
                        mediaResultBehaviour = AwesomeCamConfiguration.CONTINUE;
                        break;
                    case AwesomeCamConfiguration.PREVIEW:
                    default:
                        mediaResultBehaviour = AwesomeCamConfiguration.PREVIEW;
                        break;
                }
            }

            if (bundle.containsKey(AwesomeCamConfiguration.Arguments.CAMERA_FACE)) {
                switch (bundle.getInt(AwesomeCamConfiguration.Arguments.CAMERA_FACE)) {
                    case AwesomeCamConfiguration.CAMERA_FACE_FRONT:
                        currentCameraType = CameraSwitchView.CAMERA_TYPE_FRONT;
                        break;
                    case AwesomeCamConfiguration.CAMERA_FACE_REAR:
                    default:
                        currentCameraType = CameraSwitchView.CAMERA_TYPE_REAR;
                        break;
                }
            }

            if (bundle.containsKey(AwesomeCamConfiguration.Arguments.FILE_URI)) {
                fileUri = Uri.parse(bundle.getString(AwesomeCamConfiguration.Arguments.FILE_URI));
            }

            if (bundle.containsKey(AwesomeCamConfiguration.Arguments.MEDIA_QUALITY)) {
                switch (bundle.getInt(AwesomeCamConfiguration.Arguments.MEDIA_QUALITY)) {
                    case AwesomeCamConfiguration.MEDIA_QUALITY_AUTO:
                        mediaQuality = AwesomeCamConfiguration.MEDIA_QUALITY_AUTO;
                        break;
                    case AwesomeCamConfiguration.MEDIA_QUALITY_HIGHEST:
                        mediaQuality = AwesomeCamConfiguration.MEDIA_QUALITY_HIGHEST;
                        break;
                    case AwesomeCamConfiguration.MEDIA_QUALITY_HIGH:
                        mediaQuality = AwesomeCamConfiguration.MEDIA_QUALITY_HIGH;
                        break;
                    case AwesomeCamConfiguration.MEDIA_QUALITY_LOW:
                        mediaQuality = AwesomeCamConfiguration.MEDIA_QUALITY_LOW;
                        break;
                    case AwesomeCamConfiguration.MEDIA_QUALITY_LOWEST:
                        mediaQuality = AwesomeCamConfiguration.MEDIA_QUALITY_LOWEST;
                        break;
                    case AwesomeCamConfiguration.MEDIA_QUALITY_MEDIUM:
                    default:
                        mediaQuality = AwesomeCamConfiguration.MEDIA_QUALITY_MEDIUM;
                        break;
                }
                passedMediaQuality = mediaQuality;
            }

            if (bundle.containsKey(AwesomeCamConfiguration.Arguments.VIDEO_DURATION))
                videoDuration = bundle.getInt(AwesomeCamConfiguration.Arguments.VIDEO_DURATION);

            if (bundle.containsKey(AwesomeCamConfiguration.Arguments.VIDEO_FILE_SIZE))
                videoFileSize = bundle.getLong(AwesomeCamConfiguration.Arguments.VIDEO_FILE_SIZE);

            if (bundle.containsKey(AwesomeCamConfiguration.Arguments.MINIMUM_VIDEO_DURATION))
                minimumVideoDuration = bundle.getInt(AwesomeCamConfiguration.Arguments.MINIMUM_VIDEO_DURATION);

            if (bundle.containsKey(AwesomeCamConfiguration.Arguments.FLASH_MODE))
                switch (bundle.getInt(AwesomeCamConfiguration.Arguments.FLASH_MODE)) {
                    case AwesomeCamConfiguration.FLASH_MODE_ON:
                        flashMode = AwesomeCamConfiguration.FLASH_MODE_ON;
                        break;
                    case AwesomeCamConfiguration.FLASH_MODE_OFF:
                        flashMode = AwesomeCamConfiguration.FLASH_MODE_OFF;
                        break;
                    case AwesomeCamConfiguration.FLASH_MODE_AUTO:
                    default:
                        flashMode = AwesomeCamConfiguration.FLASH_MODE_AUTO;
                        break;
                }
        }
    }

    @Override
    protected View getUserContentView(LayoutInflater layoutInflater, ViewGroup parent) {
        cameraControlPanel = (CameraControlPanel) layoutInflater.inflate(R.layout.user_control_layout, parent, false);

        if (cameraControlPanel != null) {
            cameraControlPanel.setup(getMediaAction(), this);

            switch (flashMode) {
                case AwesomeCamConfiguration.FLASH_MODE_AUTO:
                    cameraControlPanel.setFlasMode(FlashSwitchView.FLASH_AUTO);
                    break;
                case AwesomeCamConfiguration.FLASH_MODE_ON:
                    cameraControlPanel.setFlasMode(FlashSwitchView.FLASH_ON);
                    break;
                case AwesomeCamConfiguration.FLASH_MODE_OFF:
                    cameraControlPanel.setFlasMode(FlashSwitchView.FLASH_OFF);
                    break;
            }

            cameraControlPanel.setRecordButtonListener(this);
            cameraControlPanel.setFlashModeSwitchListener(this);
            cameraControlPanel.setOnMediaActionStateChangeListener(this);
            cameraControlPanel.setOnCameraTypeChangeListener(this);
            cameraControlPanel.setMaxVideoDuration(getVideoDuration());
            cameraControlPanel.setMaxVideoFileSize(getVideoFileSize());
            cameraControlPanel.setSettingsClickListener(this);
        }

        return cameraControlPanel;
    }

    @Override
    public void onSettingsClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (currentMediaActionState == MediaActionSwitchView.ACTION_VIDEO) {
            builder.setSingleChoiceItems(videoQualities, getVideoOptionCheckedIndex(), getVideoOptionSelectedListener());
            if (getVideoFileSize() > 0)
                builder.setTitle(String.format(getString(R.string.settings_video_quality_title),
                        "(Max " + getVideoFileSize() / (1024 * 1024) + " MB)"));
            else
                builder.setTitle(String.format(getString(R.string.settings_video_quality_title), ""));
        } else {
            builder.setSingleChoiceItems(photoQualities, getPhotoOptionCheckedIndex(), getPhotoOptionSelectedListener());
            builder.setTitle(R.string.settings_photo_quality_title);
        }

        builder.setPositiveButton(R.string.ok_label, (dialogInterface, i) -> {
            if (newQuality > 0 && newQuality != mediaQuality) {
                mediaQuality = newQuality;
                dialogInterface.dismiss();
                cameraControlPanel.lockControls();
                getCameraController().switchQuality();
            }
        });
        builder.setNegativeButton(R.string.cancel_label, (dialogInterface, i) -> dialogInterface.dismiss());
        settingsDialog = builder.create();
        settingsDialog.show();
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(settingsDialog.getWindow().getAttributes());
        layoutParams.width = Utils.convertDipToPixels(this, 350);
        layoutParams.height = Utils.convertDipToPixels(this, 350);
        settingsDialog.getWindow().setAttributes(layoutParams);
    }

    @Override
    public void onCameraTypeChanged(@CameraSwitchView.CameraType int cameraType) {
        if (currentCameraType == cameraType) return;
        currentCameraType = cameraType;

        cameraControlPanel.lockControls();
        cameraControlPanel.allowRecord(false);

        int cameraFace = cameraType == CameraSwitchView.CAMERA_TYPE_FRONT
                ? AwesomeCamConfiguration.CAMERA_FACE_FRONT : AwesomeCamConfiguration.CAMERA_FACE_REAR;

        getCameraController().switchCamera(cameraFace);
    }

    @Override
    public void onFlashModeChanged(@FlashSwitchView.FlashMode int mode) {
        switch (mode) {
            case FlashSwitchView.FLASH_AUTO:
                flashMode = AwesomeCamConfiguration.FLASH_MODE_AUTO;
                getCameraController().setFlashMode(AwesomeCamConfiguration.FLASH_MODE_AUTO);
                break;
            case FlashSwitchView.FLASH_ON:
                flashMode = AwesomeCamConfiguration.FLASH_MODE_ON;
                getCameraController().setFlashMode(AwesomeCamConfiguration.FLASH_MODE_ON);
                break;
            case FlashSwitchView.FLASH_OFF:
                flashMode = AwesomeCamConfiguration.FLASH_MODE_OFF;
                getCameraController().setFlashMode(AwesomeCamConfiguration.FLASH_MODE_OFF);
                break;
        }
    }

    @Override
    public void onMediaActionChanged(int mediaActionState) {
        if (currentMediaActionState == mediaActionState) return;
        currentMediaActionState = mediaActionState;
    }

    @Override
    public void onTakePhotoButtonPressed() {
        getCameraController().takePhoto();
    }

    @Override
    public void onStartRecordingButtonPressed() {
        getCameraController().startVideoRecord();
    }

    @Override
    public void onStopRecordingButtonPressed() {
        getCameraController().stopVideoRecord();
    }

    @Override
    protected void onScreenRotation(int degrees) {
        cameraControlPanel.rotateControls(degrees);
        rotateSettingsDialog(degrees);
    }

    @Override
    public int getRequestCode() {
        return requestCode;
    }

    @Override
    public int getMediaAction() {
        return mediaAction;
    }

    @Override
    public int getMediaQuality() {
        return mediaQuality;
    }

    @Override
    public int getVideoDuration() {
        return videoDuration;
    }

    @Override
    public long getVideoFileSize() {
        return videoFileSize;
    }


    @Override
    public int getMinimumVideoDuration() {
        return minimumVideoDuration / 1000;
    }

    @Override
    public int getFlashMode() {
        return flashMode;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public int getCameraFace() {
        return currentCameraType;
    }

    @Override
    public int getMediaResultBehaviour() {
        return mediaResultBehaviour;
    }

    @Override
    public Uri getFileUri() {
        return fileUri;
    }

    @Override
    public void updateCameraPreview(Size size, View cameraPreview) {
        cameraControlPanel.unLockControls();
        cameraControlPanel.allowRecord(true);

        setCameraPreview(cameraPreview, size);
    }

    @Override
    public void updateUiForMediaAction(@AwesomeCamConfiguration.MediaAction int mediaAction) {

    }

    @Override
    public void updateCameraSwitcher(int numberOfCameras) {
        cameraControlPanel.allowCameraSwitching(numberOfCameras > 1);
    }

    @Override
    public void onPhotoTaken() {
        startPreviewActivity();
    }

    @Override
    public void onVideoRecordStart(int width, int height) {
        cameraControlPanel.onStartVideoRecord(getCameraController().getOutputMediaFile());
    }

    @Override
    public void onVideoRecordStop() {
        cameraControlPanel.allowRecord(false);
        cameraControlPanel.onStopVideoRecord();
        startPreviewActivity();
    }

    @Override
    public void releaseCameraPreview() {
        clearCameraPreview();
    }

    private void startPreviewActivity() {
        setResult(RESULT_OK);
        finish();

    }

    @Override
    public boolean isDurationEnoughToStopRecord() {
        return true;
    }

    public CameraControlPanel getCameraControlPanel() {
        return cameraControlPanel;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PREVIEW_CODE) {
                if (PreviewActivity.isResultConfirm(data)) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(AwesomeCamConfiguration.Arguments.FILE_URI,
                            PreviewActivity.getMediaFilePatch(data));
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else if (PreviewActivity.isResultCancel(data)) {
                    setResult(RESULT_CANCELED);
                    finish();
                } else if (PreviewActivity.isResultRetake(data)) {
                    //ignore, just proceed the camera
                }
            }
        }
    }

    private void rotateSettingsDialog(int degrees) {
        if (settingsDialog != null && settingsDialog.isShowing()) {
            ViewGroup dialogView = (ViewGroup) settingsDialog.getWindow().getDecorView();
            for (int i = 0; i < dialogView.getChildCount(); i++) {
                dialogView.getChildAt(i).setRotation(degrees);
            }
        }
    }

    protected abstract CharSequence[] getVideoQualityOptions();

    protected abstract CharSequence[] getPhotoQualityOptions();

    protected int getVideoOptionCheckedIndex() {
        int checkedIndex = -1;
        if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_AUTO) checkedIndex = 0;
        else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_HIGH) checkedIndex = 1;
        else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_MEDIUM) checkedIndex = 2;
        else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_LOW) checkedIndex = 3;

        if (passedMediaQuality != AwesomeCamConfiguration.MEDIA_QUALITY_AUTO) checkedIndex--;

        return checkedIndex;
    }

    protected int getPhotoOptionCheckedIndex() {
        int checkedIndex = -1;
        if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_HIGHEST) checkedIndex = 0;
        else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_HIGH) checkedIndex = 1;
        else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_MEDIUM) checkedIndex = 2;
        else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_LOWEST) checkedIndex = 3;
        return checkedIndex;
    }

    protected DialogInterface.OnClickListener getVideoOptionSelectedListener() {
        return (dialogInterface, index) -> newQuality = ((VideoQualityOption) videoQualities[index]).getMediaQuality();
    }

    protected DialogInterface.OnClickListener getPhotoOptionSelectedListener() {
        return (dialogInterface, index) -> newQuality = ((PhotoQualityOption) photoQualities[index]).getMediaQuality();
    }
}
