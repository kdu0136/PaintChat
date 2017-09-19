package com.example.kimdongun.paintchat;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;

import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.IOException;

/**
 * Created by KimDongun on 2017-09-15.
 */

public class ImageManager {
    public static Bitmap imageRotate(String imagePath, Bitmap resource){
        try {
            //이미지 회전 각 구하기
            ExifInterface exifInterface = new ExifInterface(imagePath);
            int exifOrientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int exifDegree = exifToDegrees(exifOrientation);
            DebugHandler.log("Rotation", exifDegree + "");
            //이미지 회전 각만큼 다시 돌리기
            Matrix matrix = new Matrix();
            matrix.postRotate(exifDegree);
            return Bitmap.createBitmap(resource, 0, 0, resource.getWidth(), resource.getHeight(), matrix, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resource;
    }

    public static Mat imageRotate(String imagePath, Mat mat){
        try {
            //이미지 회전 각 구하기
            ExifInterface exifInterface = new ExifInterface(imagePath);
            int exifOrientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int exifDegree = exifToDegrees(exifOrientation);
            DebugHandler.log("Rotation", exifDegree + "");

            Mat dst = new Mat();
            if(exifDegree == 180 || exifDegree == -180) {
                Core.flip(mat, dst, -1);
            } else if(exifDegree == 90 || exifDegree == -270) {
                Core.flip(mat.t(), dst, 1);
            } else if(exifDegree == 270 || exifDegree == -90) {
                Core.flip(mat.t(), dst, 0);
            }

            return dst;

//            //이미지 회전 각만큼 다시 돌리기
//            Matrix matrix = new Matrix();
//            matrix.postRotate(exifDegree);
//            Bitmap resource  = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
//            resource = Bitmap.createBitmap(resource, 0, 0, resource.getWidth(), resource.getHeight(), matrix, true);
//            Utils.bitmapToMat(resource, mat);
//            return mat;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mat;
    }

    public static int calRotate(String imagePath){
        try {
            //이미지 회전 각 구하기
            ExifInterface exifInterface = new ExifInterface(imagePath);
            int exifOrientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int exifDegree = exifToDegrees(exifOrientation);
            DebugHandler.log("Rotation", exifDegree + "");
            return exifDegree;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }
}
