package com.farukcankaya.awesomecamsample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.farukcankaya.awesomecam.AwesomeCam;
import com.farukcankaya.awesomecam.internal.configuration.AwesomeCamConfiguration;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AwesomeCamConfiguration.Builder videoLimited = new AwesomeCamConfiguration.Builder(this, 34);
        videoLimited.setMediaAction(AwesomeCamConfiguration.MEDIA_ACTION_VIDEO);
        videoLimited.setMediaQuality(AwesomeCamConfiguration.MEDIA_QUALITY_MEDIUM);
        videoLimited.setCameraFace(AwesomeCamConfiguration.CAMERA_FACE_REAR);
        //videoLimited.setFlashMode(AwesomeCamConfiguration.FLASH_MODE_AUTO);
        videoLimited.setMediaResultBehaviour(AwesomeCamConfiguration.PREVIEW);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        new AwesomeCam(videoLimited.build()).launchCamera();
    }
}
