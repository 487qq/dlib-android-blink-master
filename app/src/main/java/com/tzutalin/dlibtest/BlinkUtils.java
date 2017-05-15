package com.tzutalin.dlibtest;

import android.graphics.Point;

import java.text.DecimalFormat;

/**
 * Created by max on 2017/5/13.
 */
public class BlinkUtils {

    public static final double EYE_AR_THRESH = 0.23; //0.3 0.23
    public static final double EYE_AR_CONSEC_FRAMES = 2;

    public static double eye_aspect_ratio(Point[] eye) {
        double a = sqrtUtils(eye[1], eye[5]);  //眼睛左边上下2点
        double b = sqrtUtils(eye[2], eye[4]);   //眼睛右边上下2点
        double c = sqrtUtils(eye[0], eye[3]);   //眼睛左右2点
        double ear = (a + b) / (2 * c);

        return Double.valueOf(convert(ear));
    }

    private static double sqrtUtils(Point point1, Point point5) {
        if (null == point1 || null == point5) {
            return 0;
        }
        double size = Math.pow(point1.x - point5.x, 2) + Math.pow(point1.y - point5.y, 2);
        double len = Math.sqrt(size);
        return len;
    }

    public static String convert(double value) {
        return String.format("%.2f", value);

    }

    /**
     * get 上眼睑 和下眼睑距离
     * @param eye
     * @return
     */
    public static double eyeHigh(Point[] eye) {
        double a = sqrtUtils(eye[1], eye[5]);  //眼睛左边上下2点
        double b = sqrtUtils(eye[2], eye[4]);   //眼睛右边上下2点
        return Math.min(a,b);
    }


    public static double eyeWidth(Point[] eye){
        double c = sqrtUtils(eye[0], eye[3]);   //眼睛左右2点
        return c;
    }
}
