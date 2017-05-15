package com.tzutalin.dlibtest.camera;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "yanzi";
    CameraInterface mCameraInterface;
    Context mContext;
    SurfaceHolder mSurfaceHolder;
    private Handler mInferenceHandler;
    private CameraInterface.CamOpenOverCallback mCallback;
    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        mContext = context;
        mSurfaceHolder = getHolder();
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);//translucent半透明 transparent透明
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);
    }

    public void setParams(CameraInterface.CamOpenOverCallback callback,Handler inferenceHandler){
        this.mInferenceHandler = inferenceHandler;
        this.mCallback = callback;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        Log.i(TAG, "surfaceCreated...");
        openCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub
        Log.i(TAG, "surfaceChanged...");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        Log.i(TAG, "surfaceDestroyed...");
        CameraInterface.getInstance().doStopCamera();
    }
    public SurfaceHolder getSurfaceHolder(){
        return mSurfaceHolder;
    }


    private void openCamera(){
        Thread openThread = new Thread(){
            @Override
            public void run() {
                CameraInterface.getInstance().doOpenCamera(mCallback,mInferenceHandler,mContext);
            }
        };
        openThread.start();
    }

}