package com.tzutalin.dlibtest.camera;

import android.content.Context;
import android.graphics.*;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;
import com.tzutalin.dlibtest.BlinkUtils;
import com.tzutalin.dlibtest.DlibDemoApp;
import com.tzutalin.dlibtest.FloatingCameraWindow;
import com.tzutalin.dlibtest.ImageUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PreviewListener implements Camera.PreviewCallback {
    private Handler mInferenceHandler;
    private FaceDet mFaceDet;

    private Paint mFaceLandmardkPaint;
    private FloatingCameraWindow mWindow;
    private boolean mIsComputing = false;
    private Bitmap mFrameBitmap;

    public PreviewListener(Context context,Handler inferenceHandler) {
        this.mInferenceHandler = inferenceHandler;
        mFaceDet = DlibDemoApp.mFaceDet;

        mWindow = new FloatingCameraWindow(context);

        mFaceLandmardkPaint = new Paint();
        mFaceLandmardkPaint.setColor(Color.GREEN);
        mFaceLandmardkPaint.setStrokeWidth(2);
        mFaceLandmardkPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Size size = camera.getParameters().getPreviewSize();
        try{
            YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
            if(image!=null){
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);

                Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

                //**********************
                //因为图片会放生旋转，因此要对图片进行旋转到和手机在一个方向上
//                rotateMyBitmap(bmp);
                mFrameBitmap = ImageUtil.getRotateBitmap(bmp, -90.0f);
//                FileUtil.saveBitmap(rotaBitmap);
                //**********************************
                bmp.recycle();
                System.gc();
                if (mIsComputing) {
                    stream.close();
                    return;
                }
                mIsComputing = true;
                detect();
                stream.close();
            }
        }catch(Exception ex){
            Log.e("Sys","Error:"+ex.getMessage());
        }
    }


    private void detect(){
        mInferenceHandler.post(
                new Runnable() {
                    @Override
                    public void run() {

                        long startTime = System.currentTimeMillis();
                        List<VisionDetRet> results;
                        synchronized (PreviewListener.this) {
                            results = mFaceDet.detect(mFrameBitmap);
                        }
                        long endTime = System.currentTimeMillis();
                        Log.d("previewListener","Time cost: " + String.valueOf((endTime - startTime) / 1000f) + " sec");
                        // Draw on bitmap
                        if (results != null) {
                            for (final VisionDetRet ret : results) {
                                float resizeRatio = 1.0f;
                                Rect bounds = new Rect();
                                bounds.left = (int) (ret.getLeft() * resizeRatio);
                                bounds.top = (int) (ret.getTop() * resizeRatio);
                                bounds.right = (int) (ret.getRight() * resizeRatio);
                                bounds.bottom = (int) (ret.getBottom() * resizeRatio);
                                Canvas canvas = new Canvas(mFrameBitmap);
                                if (isDrawRect) {
                                    canvas.drawRect(bounds, mFaceLandmardkPaint);
                                }
                                Point[] rightEyes =new Point[6];
                                Point[] leftEyes =new Point[6];
                                // Draw landmark
                                ArrayList<Point> landmarks = ret.getFaceLandmarks();
                                for (int i =0; i< landmarks.size() ;i++) {
                                    Point point = landmarks.get(i);
                                    int pointX = (int) (point.x * resizeRatio);
                                    int pointY = (int) (point.y * resizeRatio);
                                    if (isDrawPoint) {
                                        canvas.drawCircle(pointX, pointY, 2, mFaceLandmardkPaint);
                                    }
                                    //36 -41 left  42-47 right
                                    if (i>=36 && i<=41){
                                        rightEyes[i-36] = point;
                                    }

                                    if (i>=42 && i<=47){
                                        leftEyes[i-42] = point;
                                    }
                                }

                                double leftEAR = BlinkUtils.eye_aspect_ratio(leftEyes);
                                double rightEAR = BlinkUtils.eye_aspect_ratio(rightEyes);
                                double ear = (leftEAR + rightEAR) / 2.0;
                                Log.w("max","ear= " + BlinkUtils.convert(ear) );
                                if (ear < BlinkUtils.EYE_AR_THRESH){
                                    count +=1;
                                }else{
//                                    if the eyes were closed for a sufficient number of
//			                            then increment the total number of blinks
                                    if (count >= BlinkUtils.EYE_AR_CONSEC_FRAMES) {
                                        total += 1;
                                        Log.w("max","blink....." + total);
//                                        ImageUtils.saveBitmap(mCroppedBitmap);
//                                        CustomToast.showToast(mContext,"blink:" + total,CustomToast.DURATION);
                                        if (isSave){
                                            ImageUtils.saveBitmap(mFrameBitmap);
//                                            saveImageHandler.sendEmptyMessageDelayed(SAVE_BITMAP,0);
                                        }
                                    }
                                    count =0;
                                }
                            }
                        }
                        if(isShowFloatWindow) {
                            mWindow.setRGBBitmap(mFrameBitmap);
                        }
//                        FileUtil.saveBitmap(bitmap);
                        mIsComputing = false;

                    }
                });
    }

    public void onPause(){
        if (null != mWindow){
            mWindow.release();
            mWindow = null;
        }
    }
    private int count = 0;
    private int total = 0;

    private static final boolean isSave = true;
    private static final boolean isDrawRect = false;
    private static final boolean isDrawPoint = false;
    private static final boolean isShowFloatWindow = true;

    private static final int SAVE_BITMAP = 0x101;
//    private  Handler saveImageHandler = new Handler(){
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            if (msg.what == SAVE_BITMAP) {
//                ImageUtils.saveBitmap(mFrameBitmap);
//                saveImageHandler.removeMessages(SAVE_BITMAP);
//            }
//        }
//    };
}
