package com.farukcankaya.awesomecam.internal.utils;

import android.annotation.TargetApi;
import android.media.Image;
import android.os.Build;
import android.util.Log;

import com.anggrayudi.storage.media.MediaFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by memfis on 7/6/16.
 */
public class ImageSaver implements Runnable {

    private final static String TAG = "ImageSaver";

    private final Image image;
    private final MediaFile mediaFile;
    private ImageSaverCallback imageSaverCallback;

    public interface ImageSaverCallback {
        void onSuccessFinish();

        void onError();
    }

    public ImageSaver(Image image, MediaFile file, ImageSaverCallback imageSaverCallback) {
        this.image = image;
        this.mediaFile = file;
        this.imageSaverCallback = imageSaverCallback;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void run() {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = null;
        try {
            output = (FileOutputStream) mediaFile.openOutputStream();
            output.write(bytes);
            imageSaverCallback.onSuccessFinish();
        } catch (IOException ignore) {
            Log.e(TAG, "Can't save the image file.");
            imageSaverCallback.onError();
        } finally {
            image.close();
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    Log.e(TAG, "Can't release image or close the output stream.");
                }
            }
        }
    }

}
