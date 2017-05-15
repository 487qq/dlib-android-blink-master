package com.tzutalin.dlibtest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.tzutalin.dlibtest.camera.*;

public class WelcomeActivity extends Activity {

    private ImageView main;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weclome);
        main = (ImageView) findViewById(R.id.main_img);
        Glide.with(this).load(R.mipmap.summer).into(main);
        initFaceDetected();
    }

    private class MyFaceDetListenre implements OnFaceDetInitListener{

        @Override
        public void onSuccess() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    go2Main();
                }
            });
        }
    }

    private void go2Main(){
        finish();
        Intent intent = new Intent(WelcomeActivity.this, com.tzutalin.dlibtest.camera.CameraActivity.class);
        startActivity(intent);
    }

    private void initFaceDetected(){
        DlibDemoApp.getInstance().init(new MyFaceDetListenre());
    }
}
