package com.farukcankaya.awesomecam.internal.manager.listener;

import com.anggrayudi.storage.media.MediaFile;

/**
 * Created by memfis on 8/14/16.
 */
public interface CameraPhotoListener {
    void onPhotoTaken(MediaFile photoMediaFile);

    void onPhotoTakeError();
}
