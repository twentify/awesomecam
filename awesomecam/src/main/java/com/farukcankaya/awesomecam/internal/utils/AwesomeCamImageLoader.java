package com.farukcankaya.awesomecam.internal.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.view.Display;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by memfis on 7/18/16.
 */
public final class AwesomeCamImageLoader {

    private static final String TAG = "AwesomeCamImageLoader";

    private final Context context;
    private Uri uri;

    private AwesomeCamImageLoader(Context context) {
        this.context = context;
    }

    public static class Builder {

        private AwesomeCamImageLoader awesomeCamImageLoader;

        public Builder(@NonNull Context context) {
            awesomeCamImageLoader = new AwesomeCamImageLoader(context);
        }

        public Builder load(Uri uri) {
            awesomeCamImageLoader.uri = uri;
            return this;
        }

        public AwesomeCamImageLoader build() {
            return awesomeCamImageLoader;
        }
    }

    public void into(final ImageView target) {
        ViewTreeObserver viewTreeObserver = target.getViewTreeObserver();
        viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                target.getViewTreeObserver().removeOnPreDrawListener(this);

                new ImageLoaderThread(target, uri).start();

                return true;
            }
        });
    }

    private class ImageLoaderThread extends Thread {

        private ImageView target;
        private Uri uri;
        private Handler mainHandler = new Handler(Looper.getMainLooper());

        private ImageLoaderThread(ImageView target, Uri uri) {
            this.target = target;
            this.uri = uri;
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();

            int imageViewHeight;
            int imageViewWidth;

            Point size = new Point();
            display.getSize(size);
            imageViewHeight = size.y;
            imageViewWidth = size.x;

            FileDescriptor fd = null;
            try {
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "rw");
                fd = pfd.getFileDescriptor();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            Bitmap decodedBitmap = decodeSampledBitmapFromResource(fd, imageViewWidth, imageViewHeight);
            final Bitmap resultBitmap = rotateBitmap(decodedBitmap, getExifOrientation(fd));

            mainHandler.post(() -> target.setImageBitmap(resultBitmap));
        }

        private Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.setScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.setRotate(180);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.setRotate(90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setRotate(-90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(-90);
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    return bitmap;
            }

            try {
                Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap.recycle();
                return bmRotated;
            } catch (OutOfMemoryError ignore) {
                return null;
            }
        }

        private int getExifOrientation(FileDescriptor fd) {
            ExifInterface exif = null;
            try {
                exif = new ExifInterface(fd);
            } catch (IOException ignore) {
            }
            return exif == null ? ExifInterface.ORIENTATION_UNDEFINED :
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        }

        private Bitmap decodeSampledBitmapFromResource(FileDescriptor fd,
                                                       int requestedWidth, int requestedHeight) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(fd, null, options);

            options.inSampleSize = calculateInSampleSize(options, requestedWidth, requestedHeight);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFileDescriptor(fd, null, options);
        }

        private int calculateInSampleSize(BitmapFactory.Options options,
                                          int requestedWidth, int requestedHeight) {
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > requestedHeight || width > requestedWidth) {

                final int halfHeight = height / inSampleSize;
                final int halfWidth = width / inSampleSize;

                while ((halfHeight / inSampleSize) > requestedHeight
                        && (halfWidth / inSampleSize) > requestedWidth) {
                    inSampleSize *= 2;
                }
            }
            return inSampleSize;
        }
    }

}

