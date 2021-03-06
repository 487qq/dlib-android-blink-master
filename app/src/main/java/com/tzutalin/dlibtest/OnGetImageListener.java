/*
 * Copyright 2016-present Tzutalin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tzutalin.dlibtest;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.Message;
import android.os.Trace;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;

import android.widget.Toast;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import junit.framework.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that takes in preview frames and converts the image to Bitmaps to process with dlib lib.
 */
public class OnGetImageListener implements OnImageAvailableListener {
    private static final boolean SAVE_PREVIEW_BITMAP = false;


    private static final int INPUT_SIZE = 224;
    private static final String TAG = "OnGetImageListener";

    private int mScreenRotation = 90;

    private int mPreviewWdith = 0;
    private int mPreviewHeight = 0;
    private byte[][] mYUVBytes;
    private int[] mRGBBytes = null;
    private Bitmap mRGBframeBitmap = null;
    private Bitmap mCroppedBitmap = null;

    private boolean mIsComputing = false;
    private Handler mInferenceHandler;

    private Context mContext;
    private FaceDet mFaceDet;
    private TrasparentTitleView mTransparentTitleView;
    private FloatingCameraWindow mWindow;
    private Paint mFaceLandmardkPaint;


    public void initialize(
            final Context context,
            final AssetManager assetManager,
            final TrasparentTitleView scoreView,
            final Handler handler) {
        this.mContext = context;
        this.mTransparentTitleView = scoreView;
        this.mInferenceHandler = handler;
        mFaceDet = DlibDemoApp.mFaceDet;
        mWindow = new FloatingCameraWindow(mContext);

        mFaceLandmardkPaint = new Paint();
        mFaceLandmardkPaint.setColor(Color.GREEN);
        mFaceLandmardkPaint.setStrokeWidth(2);
        mFaceLandmardkPaint.setStyle(Paint.Style.STROKE);
    }



    public void deInitialize() {
        synchronized (OnGetImageListener.this) {
//            if (mFaceDet != null) {
//                mFaceDet.release();
//            }

            if (mWindow != null) {
                mWindow.release();
            }
        }
    }

    private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {

        Display getOrient = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = Configuration.ORIENTATION_UNDEFINED;
        Point point = new Point();
        getOrient.getSize(point);
        int screen_width = point.x;
        int screen_height = point.y;
        Log.d(TAG, String.format("screen size (%d,%d)", screen_width, screen_height));
        if (screen_width < screen_height) {
            orientation = Configuration.ORIENTATION_PORTRAIT;
            mScreenRotation = -90;
        } else {
            orientation = Configuration.ORIENTATION_LANDSCAPE;
            mScreenRotation = 0;
        }

//        Assert.assertEquals(dst.getWidth(), dst.getHeight());
        final float minDim = Math.min(src.getWidth(), src.getHeight());

        final Matrix matrix = new Matrix();

        // We only want the center square out of the original rectangle.
        final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
        final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        matrix.preTranslate(translateX, translateY);

        final float scaleFactor = dst.getHeight() / minDim;
        matrix.postScale(scaleFactor, scaleFactor);
//
//        // Rotate around the center if necessary.
        if (mScreenRotation != 0) {
            matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
            matrix.postRotate(mScreenRotation);
            matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
        }

        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = null;
        try {
            image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            // No mutex needed as this method is not reentrant.
            if (mIsComputing) {
                image.close();
                return;
            }
            mIsComputing = true;

            Trace.beginSection("imageAvailable");

            final Plane[] planes = image.getPlanes();

            // Initialize the storage bitmaps once when the resolution is known.
            if (mPreviewWdith != image.getWidth() || mPreviewHeight != image.getHeight()) {
                mPreviewWdith = image.getWidth();
                mPreviewHeight = image.getHeight();

                Log.d(TAG, String.format("Initializing at size %dx%d", mPreviewWdith, mPreviewHeight));
                mRGBBytes = new int[mPreviewWdith * mPreviewHeight];
                mRGBframeBitmap = Bitmap.createBitmap(mPreviewWdith, mPreviewHeight, Config.ARGB_8888);
                mCroppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);

                mYUVBytes = new byte[planes.length][];
                for (int i = 0; i < planes.length; ++i) {
                    mYUVBytes[i] = new byte[planes[i].getBuffer().capacity()];
                }
            }

            for (int i = 0; i < planes.length; ++i) {
                planes[i].getBuffer().get(mYUVBytes[i]);
            }

            final int yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();
            ImageUtils.convertYUV420ToARGB8888(
                    mYUVBytes[0],
                    mYUVBytes[1],
                    mYUVBytes[2],
                    mRGBBytes,
                    mPreviewWdith,
                    mPreviewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    false);

            image.close();
        } catch (final Exception e) {
            if (image != null) {
                image.close();
            }
            Log.e(TAG, "Exception!", e);
            Trace.endSection();
            return;
        }

        mRGBframeBitmap.setPixels(mRGBBytes, 0, mPreviewWdith, 0, 0, mPreviewWdith, mPreviewHeight);
        drawResizedBitmap(mRGBframeBitmap, mCroppedBitmap);

        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(mCroppedBitmap);
        }

        mInferenceHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (null == mFaceDet)
                        {
                            mIsComputing = false;
                            return;
                        }
                        if (!new File(Constants.getFaceShapeModelPath()).exists()) {
                            mTransparentTitleView.setText("Copying landmark model to " + Constants.getFaceShapeModelPath());
                            FileUtils.copyFileFromRawToOthers(mContext, R.raw.shape_predictor_68_face_landmarks, Constants.getFaceShapeModelPath());
                        }

                        long startTime = System.currentTimeMillis();
                        List<VisionDetRet> results;
                        synchronized (OnGetImageListener.this) {
                            results = mFaceDet.detect(mCroppedBitmap);
                        }
                        long endTime = System.currentTimeMillis();
                        mTransparentTitleView.setText("Time cost: " + String.valueOf((endTime - startTime) / 1000f) + " sec");
                        // Draw on bitmap
                        if (results != null) {
                            for (final VisionDetRet ret : results) {
                                float resizeRatio = 1.0f;
                                Rect bounds = new Rect();
                                bounds.left = (int) (ret.getLeft() * resizeRatio);
                                bounds.top = (int) (ret.getTop() * resizeRatio);
                                bounds.right = (int) (ret.getRight() * resizeRatio);
                                bounds.bottom = (int) (ret.getBottom() * resizeRatio);
                                Canvas canvas = new Canvas(mCroppedBitmap);
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

//                                if (preLeftEar !=0 && preRightEar !=0 ){   //平视手机屏幕
//                                    Log.w("max","left: " + leftEAR + "   right: " + rightEAR );
//                                    Log.w("max","pre left: " + preLeftEar + "   right: " + preRightEar );
//                                    boolean isNowClose = leftEAR < BlinkUtils.EYE_AR_THRESH && rightEAR <BlinkUtils.EYE_AR_THRESH;
//                                    boolean isPreOpen = preLeftEar >= BlinkUtils.EYE_AR_THRESH && preRightEar>=BlinkUtils.EYE_AR_THRESH;
//                                    if (isNowClose && isPreOpen){
//                                        total += 1;
//                                        CustomToast.showToast(mContext,"blink:" + total,CustomToast.DURATION);
//                                        if (isSave){
//                                            saveImageHandler.sendEmptyMessageDelayed(SAVE_BITMAP,1000);
//
//                                        }
//                                    }
//
//                                }
//                                preLeftEar = leftEAR;
//                                preRightEar = rightEAR;

//
//                                Log.w("max","ear= " + BlinkUtils.convert(ear) );
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
                                            saveImageHandler.sendEmptyMessageDelayed(SAVE_BITMAP,0);
                                        }
                                    }
                                    count =0;
                                }

                            }
                        }
                        if (isShowFloatWindow) {
                            mWindow.setRGBBitmap(mCroppedBitmap);
                        }
                        mIsComputing = false;
                    }
                });

        Trace.endSection();
    }

    private int count = 0;
    private int total = 0;


    private double preLeftEar;
    private double preRightEar;

    private static final boolean isSave = true;
    private static final boolean isDrawRect = false;
    private static final boolean isDrawPoint = false;
    private static final boolean isShowFloatWindow = false;

    private static final int SAVE_BITMAP = 0x101;
    private  Handler saveImageHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == SAVE_BITMAP) {
                ImageUtils.saveBitmap(mRGBframeBitmap);
                saveImageHandler.removeMessages(SAVE_BITMAP);
            }
        }
    };
}
