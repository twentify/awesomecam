package com.farukcankaya.awesomecam.internal.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.CamcorderProfile;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.anggrayudi.storage.file.MimeType;
import com.anggrayudi.storage.media.FileDescription;
import com.anggrayudi.storage.media.MediaFile;
import com.anggrayudi.storage.media.MediaStoreCompat;
import com.farukcankaya.awesomecam.internal.configuration.AwesomeCamConfiguration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by memfis on 7/6/16.
 * <p/>
 * Class with some common methods to work with camera.
 */
public final class CameraHelper {

    public final static String TAG = "CameraHelper";


    private CameraHelper() {

    }

    public static boolean hasCamera(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean hasCamera2(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false;
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String[] idList = manager.getCameraIdList();
            boolean notNull = true;
            if (idList.length == 0) {
                notNull = false;
            } else {
                for (final String str : idList) {
                    if (str == null || str.trim().isEmpty()) {
                        notNull = false;
                        break;
                    }
                    final CameraCharacteristics characteristics = manager.getCameraCharacteristics(str);

                    final int supportLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    if (supportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        notNull = false;
                        break;
                    }
                }
            }
            return notNull;
        } catch (Throwable ignore) {
            return false;
        }
    }

    public static MediaFile getOutputMediaFile(Context context, @AwesomeCamConfiguration.MediaAction int mediaAction) {
        String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
        MediaFile mediaFile;
        if (mediaAction == AwesomeCamConfiguration.MEDIA_ACTION_PHOTO) {
            mediaFile = MediaStoreCompat.createImage(context, new FileDescription("IMG_" + timeStamp + ".jpg", "", MimeType.IMAGE));
        } else if (mediaAction == AwesomeCamConfiguration.MEDIA_ACTION_VIDEO) {
            mediaFile = MediaStoreCompat.createVideo(context, new FileDescription("VID_" + timeStamp + ".mp4", "", MimeType.VIDEO));
        } else {
            return null;
        }

        return mediaFile;
    }

    @SuppressWarnings("deprecation")
    public static Size getPictureSize(List<Size> choices, @AwesomeCamConfiguration.MediaQuality int mediaQuality) {
        if (choices == null || choices.isEmpty()) return null;
        if (choices.size() == 1) return choices.get(0);

        Size result = null;
        Size maxPictureSize = Collections.max(choices, new CompareSizesByArea2());
        Size minPictureSize = Collections.min(choices, new CompareSizesByArea2());

        Collections.sort(choices, new CompareSizesByArea2());

        if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_HIGHEST) {
            result = maxPictureSize;
        } else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_LOW) {
            if (choices.size() == 2) result = minPictureSize;
            else {
                int half = choices.size() / 2;
                int lowQualityIndex = (choices.size() - half) / 2;
                result = choices.get(lowQualityIndex + 1);
            }
        } else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_HIGH) {
            if (choices.size() == 2) result = maxPictureSize;
            else {
                int half = choices.size() / 2;
                int highQualityIndex = (choices.size() - half) / 2;
                result = choices.get(choices.size() - highQualityIndex - 1);
            }
        } else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_MEDIUM) {
            if (choices.size() == 2) result = minPictureSize;
            else {
                int mediumQualityIndex = choices.size() / 2;
                result = choices.get(mediumQualityIndex);
            }
        } else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_LOWEST) {
            result = minPictureSize;
        }

        return result;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Size getPictureSize(Size[] sizes, @AwesomeCamConfiguration.MediaQuality int mediaQuality) {
        if (sizes == null || sizes.length == 0) return null;

        List<Size> choices = Arrays.asList(sizes);

        if (choices.size() == 1) return choices.get(0);

        Size result = null;
        Size maxPictureSize = Collections.max(choices, new CompareSizesByArea2());
        Size minPictureSize = Collections.min(choices, new CompareSizesByArea2());

        Collections.sort(choices, new CompareSizesByArea2());

        if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_HIGHEST) {
            result = maxPictureSize;
        } else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_LOW) {
            if (choices.size() == 2) result = minPictureSize;
            else {
                int half = choices.size() / 2;
                int lowQualityIndex = (choices.size() - half) / 2;
                result = choices.get(lowQualityIndex + 1);
            }
        } else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_HIGH) {
            if (choices.size() == 2) result = maxPictureSize;
            else {
                int half = choices.size() / 2;
                int highQualityIndex = (choices.size() - half) / 2;
                result = choices.get(choices.size() - highQualityIndex - 1);
            }
        } else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_MEDIUM) {
            if (choices.size() == 2) result = minPictureSize;
            else {
                int mediumQualityIndex = choices.size() / 2;
                result = choices.get(mediumQualityIndex);
            }
        } else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_LOWEST) {
            result = minPictureSize;
        }

        return result;
    }

    @SuppressWarnings("deprecation")
    public static Size getOptimalPreviewSize(List<Size> sizes, int width, int height) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) height / width;

        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Size size : sizes) {
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.getHeight() - height) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - height);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - height) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - height);
                }
            }
        }
        return optimalSize;
    }

    @SuppressWarnings("deprecation")
    public static Size getSizeWithClosestRatio(List<Size> sizes, int width, int height) {

        if (sizes == null) return null;

        double MIN_TOLERANCE = 100;
        double targetRatio = (double) height / width;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Size size : sizes) {
            if (size.getWidth() == width && size.getHeight() == height)
                return size;

            double ratio = (double) size.getHeight() / size.getWidth();

            if (Math.abs(ratio - targetRatio) < MIN_TOLERANCE) MIN_TOLERANCE = ratio;
            else continue;

            if (Math.abs(size.getHeight() - height) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - height);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - height) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - height);
                }
            }
        }
        return optimalSize;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Size getOptimalPreviewSize(Size[] sizes, int width, int height) {

        if (sizes == null) return null;

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) height / width;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Size size : sizes) {
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.getHeight() - height) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - height);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - height) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - height);
                }
            }
        }
        return optimalSize;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Size getSizeWithClosestRatio(Size[] sizes, int width, int height) {

        if (sizes == null) return null;

        double MIN_TOLERANCE = 100;
        double targetRatio = (double) height / width;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Size size : sizes) {

            double ratio = (double) size.getHeight() / size.getWidth();

            if (Math.abs(ratio - targetRatio) < MIN_TOLERANCE) MIN_TOLERANCE = ratio;
            else continue;

            if (Math.abs(size.getHeight() - height) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - height);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - height) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - height);
                }
            }
        }
        return optimalSize;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea2());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return null;
        }
    }

    private static double calculateApproximateVideoSize(CamcorderProfile camcorderProfile, int seconds) {
        return ((camcorderProfile.videoBitRate + camcorderProfile.audioBitRate) * seconds) / (float) 8;
    }

    public static double calculateApproximateVideoDuration(CamcorderProfile camcorderProfile, long maxFileSize) {
        return 8 * maxFileSize / (camcorderProfile.videoBitRate + camcorderProfile.audioBitRate);
    }

    private static long calculateMinimumRequiredBitRate(CamcorderProfile camcorderProfile, long maxFileSize, int seconds) {
        return 8 * maxFileSize / seconds - camcorderProfile.audioBitRate;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static CamcorderProfile getCamcorderProfile(String cameraId, long maximumFileSize, int minimumDurationInSeconds) {
        if (TextUtils.isEmpty(cameraId)) {
            return null;
        }
        int cameraIdInt = Integer.parseInt(cameraId);
        return getCamcorderProfile(cameraIdInt, maximumFileSize, minimumDurationInSeconds);
    }

    public static CamcorderProfile getCamcorderProfile(int currentCameraId, long maximumFileSize, int minimumDurationInSeconds) {
        if (maximumFileSize <= 0)
            return CamcorderProfile.get(currentCameraId, AwesomeCamConfiguration.MEDIA_QUALITY_HIGHEST);

        int[] qualities = new int[]{AwesomeCamConfiguration.MEDIA_QUALITY_HIGHEST,
                AwesomeCamConfiguration.MEDIA_QUALITY_HIGH, AwesomeCamConfiguration.MEDIA_QUALITY_MEDIUM,
                AwesomeCamConfiguration.MEDIA_QUALITY_LOW, AwesomeCamConfiguration.MEDIA_QUALITY_LOWEST};

        CamcorderProfile camcorderProfile;
        for (int quality : qualities) {
            camcorderProfile = CameraHelper.getCamcorderProfile(quality, currentCameraId);
            double fileSize = CameraHelper.calculateApproximateVideoSize(camcorderProfile, minimumDurationInSeconds);

            if (fileSize > maximumFileSize) {
                long minimumRequiredBitRate = calculateMinimumRequiredBitRate(camcorderProfile, maximumFileSize, minimumDurationInSeconds);

                if (minimumRequiredBitRate >= camcorderProfile.videoBitRate / 4 && minimumRequiredBitRate <= camcorderProfile.videoBitRate) {
                    camcorderProfile.videoBitRate = (int) minimumRequiredBitRate;
                    return camcorderProfile;
                }
            } else return camcorderProfile;
        }
        return CameraHelper.getCamcorderProfile(AwesomeCamConfiguration.MEDIA_QUALITY_LOWEST, currentCameraId);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static CamcorderProfile getCamcorderProfile(@AwesomeCamConfiguration.MediaQuality int mediaQuality, String cameraId) {
        if (TextUtils.isEmpty(cameraId)) {
            return null;
        }
        int cameraIdInt = Integer.parseInt(cameraId);
        return getCamcorderProfile(mediaQuality, cameraIdInt);
    }

    public static CamcorderProfile getCamcorderProfile(@AwesomeCamConfiguration.MediaQuality int mediaQuality, int cameraId) {
        if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_HIGHEST) {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
        } else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_HIGH) {
            if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P)) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_1080P);
            } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P);
            } else {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
            }
        } else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_MEDIUM) {
            if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P);
            } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
            } else {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
            }
        } else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_LOW) {
            if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
            } else {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
            }
        } else if (mediaQuality == AwesomeCamConfiguration.MEDIA_QUALITY_LOWEST) {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
        } else {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static class CompareSizesByArea2 implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
}
