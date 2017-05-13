package com.tzutalin.dlibtest;

import android.graphics.Point;

/**
 * Created by max on 2017/5/13.
 */
public class BlinkUtils {

    public static final double EYE_AR_THRESH = 0.23; //0.3
    public static final double EYE_AR_CONSEC_FRAMES = 2;

    public static double eye_aspect_ratio(Point[] eye){
        double a = sqrtUtils(eye[1],eye[5]);
        double b =sqrtUtils(eye[2],eye[4]);
        double c = sqrtUtils(eye[0],eye[3]);
        double ear = (a+b)/(2*c);
        return ear;
    }

    private static double sqrtUtils(Point point1,Point point5){
        double size = Math.pow(point1.x-point5.x,2)+ Math.pow(point1.y-point5.y,2);
        double len = Math.sqrt(size);
        return len;
    }

    public static   String   convert(double   value){
       return String.format("%.2f",value);

    }
}
