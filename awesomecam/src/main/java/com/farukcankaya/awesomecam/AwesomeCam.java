package com.farukcankaya.awesomecam;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.support.annotation.IntRange;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.Fragment;

import com.farukcankaya.awesomecam.internal.configuration.AwesomeCamConfiguration;
import com.farukcankaya.awesomecam.internal.ui.camera.Camera1Activity;
import com.farukcankaya.awesomecam.internal.ui.camera2.Camera2Activity;
import com.farukcankaya.awesomecam.internal.utils.CameraHelper;

/**
 * Created by memfis on 7/6/16.
 */
public class AwesomeCam {

    private AwesomeCamConfiguration awesomeCamConfiguration;

    /***
     * Creates AwesomeCam instance with default configuration set to photo with medium quality.
     *
     * @param activity    - fromList which request was invoked
     * @param requestCode - request code which will return in onActivityForResult
     */
    public AwesomeCam(Activity activity, @IntRange(from = 0) int requestCode) {
        AwesomeCamConfiguration.Builder builder = new AwesomeCamConfiguration.Builder(activity, requestCode);
        awesomeCamConfiguration = builder.build();
    }

    public AwesomeCam(Fragment fragment, @IntRange(from = 0) int requestCode) {
        AwesomeCamConfiguration.Builder builder = new AwesomeCamConfiguration.Builder(fragment, requestCode);
        awesomeCamConfiguration = builder.build();
    }

    /***
     * Creates AwesomeCam instance with custom camera configuration.
     *
     * @param cameraConfiguration
     */
    public AwesomeCam(AwesomeCamConfiguration cameraConfiguration) {
        this.awesomeCamConfiguration = cameraConfiguration;
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public void launchCamera() {
        launchCamera(Camera1Activity.class, Camera2Activity.class);
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public void launchCamera(Class<?> camera1Class, Class<?> camera2Class) {
        if (awesomeCamConfiguration == null || (awesomeCamConfiguration.getActivity() == null && awesomeCamConfiguration.getFragment() == null))
            return;

        Intent cameraIntent;

        if (CameraHelper.hasCamera2(awesomeCamConfiguration.getActivity())) {
            if (awesomeCamConfiguration.getFragment() != null)
                cameraIntent = new Intent(awesomeCamConfiguration.getFragment().getContext(), camera2Class);
            else cameraIntent = new Intent(awesomeCamConfiguration.getActivity(), camera2Class);
        } else {
            if (awesomeCamConfiguration.getFragment() != null)
                cameraIntent = new Intent(awesomeCamConfiguration.getFragment().getContext(), camera1Class);
            else cameraIntent = new Intent(awesomeCamConfiguration.getActivity(), camera1Class);
        }

        cameraIntent.putExtra(AwesomeCamConfiguration.Arguments.REQUEST_CODE, awesomeCamConfiguration.getRequestCode());
        cameraIntent.putExtra(AwesomeCamConfiguration.Arguments.CAMERA_FACE, awesomeCamConfiguration.getCameraFace());
        cameraIntent.putExtra(AwesomeCamConfiguration.Arguments.FILE_PATH, awesomeCamConfiguration.getOutPutFilePath());
        cameraIntent.putExtra(AwesomeCamConfiguration.Arguments.MEDIA_RESULT_BEHAVIOUR, awesomeCamConfiguration.getMediaResultBehaviour());
        cameraIntent.putExtra(AwesomeCamConfiguration.Arguments.IS_PREVIEW_REQUIRE_CONFIRMATION, awesomeCamConfiguration.isRequireConfirmation());

        if (awesomeCamConfiguration.getMediaAction() > 0)
            cameraIntent.putExtra(AwesomeCamConfiguration.Arguments.MEDIA_ACTION, awesomeCamConfiguration.getMediaAction());

        if (awesomeCamConfiguration.getMediaQuality() > 0)
            cameraIntent.putExtra(AwesomeCamConfiguration.Arguments.MEDIA_QUALITY, awesomeCamConfiguration.getMediaQuality());

        if (awesomeCamConfiguration.getVideoDuration() > 0)
            cameraIntent.putExtra(AwesomeCamConfiguration.Arguments.VIDEO_DURATION, awesomeCamConfiguration.getVideoDuration());

        if (awesomeCamConfiguration.getVideoFileSize() > 0)
            cameraIntent.putExtra(AwesomeCamConfiguration.Arguments.VIDEO_FILE_SIZE, awesomeCamConfiguration.getVideoFileSize());

        if (awesomeCamConfiguration.getMinimumVideoDuration() > 0)
            cameraIntent.putExtra(AwesomeCamConfiguration.Arguments.MINIMUM_VIDEO_DURATION, awesomeCamConfiguration.getMinimumVideoDuration());

        cameraIntent.putExtra(AwesomeCamConfiguration.Arguments.FLASH_MODE, awesomeCamConfiguration.getFlashMode());

        if (awesomeCamConfiguration.getFragment() != null) {

            awesomeCamConfiguration.getFragment().startActivityForResult(cameraIntent, awesomeCamConfiguration.getRequestCode());
        } else {
            awesomeCamConfiguration.getActivity().startActivityForResult(cameraIntent, awesomeCamConfiguration.getRequestCode());
        }
    }
}
