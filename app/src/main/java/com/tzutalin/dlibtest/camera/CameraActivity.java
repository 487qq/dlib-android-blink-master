package com.tzutalin.dlibtest.camera;


import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import com.tzutalin.dlibtest.CameraConnectionFragment;
import com.tzutalin.dlibtest.R;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CameraActivity extends Activity implements CameraInterface.CamOpenOverCallback {
    private static final String TAG = "yanzi";
    CameraSurfaceView surfaceView = null;
    Button shutterBtn;
    float previewRate = -1f;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBackgroudThread();
//        openCamera();
        setContentView(R.layout.activity_camera);
        initUI();
        initViewParams();

        shutterBtn.setOnClickListener(new BtnListeners());
    }

    private  HandlerThread inferenceThread;
    private  Handler inferenceHandler;
    private void initBackgroudThread(){
        inferenceThread = new HandlerThread("InferenceThread");
        inferenceThread.start();
        inferenceHandler = new Handler(inferenceThread.getLooper());
    }

    private void initUI(){
        surfaceView = (CameraSurfaceView)findViewById(R.id.camera_surfaceview);
        surfaceView.setParams(this,inferenceHandler);
        shutterBtn = (Button)findViewById(R.id.btn_shutter);
    }
    private void initViewParams(){
        LayoutParams params = surfaceView.getLayoutParams();
        Point p = DisplayUtil.getScreenMetrics(this);
        params.width = p.x;
        params.height = p.y;
        previewRate = DisplayUtil.getScreenRate(this); //默认全屏的比例预览
        surfaceView.setLayoutParams(params);

        //手动设置拍照ImageButton的大小为120dip×120dip,原图片大小是64×64
        LayoutParams p2 = shutterBtn.getLayoutParams();
        p2.width = DisplayUtil.dip2px(this, 80);
        p2.height = DisplayUtil.dip2px(this, 80);;
        shutterBtn.setLayoutParams(p2);

    }



    @Override
    public void cameraHasOpened() {
        // TODO Auto-generated method stub
        SurfaceHolder holder = surfaceView.getSurfaceHolder();
        CameraInterface.getInstance().doStartPreview(surfaceView,holder, previewRate);
    }
    private class BtnListeners implements OnClickListener{

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            switch(v.getId()){
                case R.id.btn_shutter:
                    CameraInterface.getInstance().doTakePicture();
                    break;
                default:break;
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        CameraInterface.getInstance().onPause();
    }
}