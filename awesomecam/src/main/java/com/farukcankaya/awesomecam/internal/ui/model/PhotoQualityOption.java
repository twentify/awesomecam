package com.farukcankaya.awesomecam.internal.ui.model;

import com.farukcankaya.awesomecam.internal.configuration.AwesomeCamConfiguration;
import com.farukcankaya.awesomecam.internal.utils.Size;

/**
 * Created by memfis on 12/1/16.
 */

public class PhotoQualityOption implements CharSequence {

    @AwesomeCamConfiguration.MediaQuality
    private int mediaQuality;
    private String title;

    public PhotoQualityOption(@AwesomeCamConfiguration.MediaQuality int mediaQuality, Size size) {
        this.mediaQuality = mediaQuality;

        title = size.getWidth() + " x " + size.getHeight();
    }

    @AwesomeCamConfiguration.MediaQuality
    public int getMediaQuality() {
        return mediaQuality;
    }

    @Override
    public int length() {
        return title.length();
    }

    @Override
    public char charAt(int index) {
        return title.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return title.subSequence(start, end);
    }

    @Override
    public String toString() {
        return title;
    }
}
