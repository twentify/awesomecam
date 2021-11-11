package com.farukcankaya.awesomecam.internal.manager.listener;

import com.anggrayudi.storage.media.MediaFile;
import com.farukcankaya.awesomecam.internal.utils.Size;

/**
 * Created by memfis on 8/14/16.
 */
public interface CameraVideoListener {
    void onVideoRecordStarted(Size videoSize);

    void onVideoRecordStopped(MediaFile videoFile);

    void onVideoRecordError();
}
