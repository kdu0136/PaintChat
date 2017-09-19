package com.example.kimdongun.paintchat.adapter;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.view.View;
import android.widget.ImageView;

import com.example.kimdongun.paintchat.item.CanvasRect;
import com.example.kimdongun.paintchat.item.MaskItem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 * Created by KimDongun on 2017-09-08.
 */

public class MaskManager {
    public boolean myMaskVisible; //내 마스크 목록 보일지 말지
    public boolean myMaskTouched; //마스크 터치 유무
    public HashMap<Integer, MaskItem> maskList_ = new HashMap<>(); //마스크 리스트 (방위각(위치) 저장)

    public MaskManager(boolean myMaskVisible){
        this.myMaskVisible = myMaskVisible;
    }

    public void putMask(int id, MaskItem mask){
        mask.azimuth = 30 * maskList_.size(); //마스크가 위치할 각도 입력
        mask.azimuth = mask.azimuth % 360;
        mask.floor = maskList_.size()/12; //한 층에 12개 마스크 위치 하기 위함

        maskList_.put(id, mask); //새로운 마스크 추가
    }

    public void putMask(int id, MaskItem mask, float azimuth){
        mask.azimuth = azimuth; //마스크가 위치할 각도 입력
        mask.floor = 0; //한 층에 12개 마스크 위치 하기 위함

        maskList_.put(id, mask); //새로운 마스크 추가
    }

    public MaskItem getMask(int id){
        return maskList_.get(id);
    }

    public void removeMask(int rm_id){
        maskList_.remove(rm_id);

        Vector v = new Vector(maskList_.keySet());
        Collections.sort(v);
        Iterator iterator = v.iterator();
        int count = 0;
        while(iterator.hasNext()){
            int id = (int)iterator.next();
            MaskItem mask = maskList_.get(id);
            mask.azimuth = 30 * count++; //현재 보고 있는 각을 기준으로 새로 새팅
            mask.azimuth = mask.azimuth % 360;
            mask.floor = maskList_.size()/12; //한 층에 12개 마스크 위치 하기 위함
        }
    }

    //보이게 시작 했을 때 정면 방위각 고려해서 마스크 보이게 하는 함수
    public void setVisibleMyMask(boolean visible, float initAzimuth){
        myMaskVisible = visible;
        if(!myMaskVisible){ //안보이게 했을 경우
            Iterator iterator = maskList_.keySet().iterator();
            while(iterator.hasNext()){
                int id = (int)iterator.next();
                maskList_.get(id).imageView.setVisibility(View.GONE);
            }
        }else{ //보이게 했을 경우
            Vector v = new Vector(maskList_.keySet());
            Collections.sort(v);
            Iterator iterator = v.iterator();
            int count = 0;
            while(iterator.hasNext()){
                int id = (int)iterator.next();
                MaskItem mask = maskList_.get(id);
                mask.azimuth = initAzimuth + 30 * count++; //현재 보고 있는 각을 기준으로 새로 새팅
                mask.azimuth = mask.azimuth % 360;
            }
        }
    }

    public void setMaskTouch(boolean touch){
        myMaskTouched = touch;
        if(myMaskTouched) { //마스크를 터치 했을 경우 (touch ON)
            Iterator iterator = maskList_.keySet().iterator();
            while (iterator.hasNext()) {
                int id = (int) iterator.next();
                maskList_.get(id).imageView.setVisibility(View.GONE);
            }
        }
    }

    public void calVisibleBound(float azimuth, float fov){
        //0- 왼쪽 시야 값이 정면 시야 값보다 작고 오른쪽 시야 값이 정면 시야값보다 큰 경우 ex) 30 - 60 - 90
        //1- 왼쪽 시야 값이 정면 시야 값보다 크고 오른쪽 시야 값이 정면 시야값보다 큰 경우 ex) 330 - 0 - 30
        //1- 왼쪽 시야 값이 정면 시야 값보다 작고 오른쪽 시야 값이 정면 시야값보다 작은 경우 ex) 320 - 350 - 20
        int angleStatus = 0;

        float leftEnd = azimuth - fov; //왼쪽 시야 끝
        if(leftEnd < 0){
            leftEnd += 360;
            angleStatus = 1;
        }

        float rightEnd = azimuth + fov; //오른쪽 시야 끝
        if(rightEnd > 360){
            rightEnd -= 360;
            angleStatus = 1;
        }

        Iterator iterator = maskList_.keySet().iterator();
        while (iterator.hasNext()){
            int id = (int)iterator.next();
            MaskItem mask = maskList_.get(id);
            float maskAzimuth = mask.azimuth;
            switch (angleStatus) {
                case 0:
                    if (leftEnd < maskAzimuth && maskAzimuth < rightEnd) {
                        //마스크 보이게
                        mask.imageView.setVisibility(View.VISIBLE);
                    } else {
                        //마스크 안 보이게
                        mask.imageView.setVisibility(View.GONE);
                    }
                    break;
                case 1:
                    if ((leftEnd < maskAzimuth && maskAzimuth <= 360) || (0 <= maskAzimuth && maskAzimuth < rightEnd)) {
                        //마스크 보이게
                        mask.imageView.setVisibility(View.VISIBLE);
                    } else {
                        //마스크 안 보이게
                        mask.imageView.setVisibility(View.GONE);
                    }
                    break;
            }
        }
    }

    public void moveMaskX(final int centerX, final float azimuth, final float cameraVerticalAngle){
        synchronized (this) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Iterator iterator = maskList_.keySet().iterator();
                    while (iterator.hasNext()) {
                        int id = (int) iterator.next();
                        MaskItem mask = maskList_.get(id);
                        double roundAzimuth = Math.round(azimuth);
                        roundAzimuth = roundAzimuth - mask.azimuth; //각도 차이 구함
                        roundAzimuth = Math.toRadians(roundAzimuth); //좌우 회전 각 (소수점 다 날려버림) -> Radian 값으로
                        try {
                            double increaseWidth = (Math.sin(roundAzimuth) / Math.sin(Math.toRadians(cameraVerticalAngle))) * centerX * 1.5f;
                            increaseWidth = Math.round(increaseWidth);
                            double newWidth = centerX - increaseWidth;
                            Message msg = handlerX.obtainMessage();
                            msg.arg1 = id;
                            msg.obj = String.valueOf(newWidth);
                            handlerX.sendMessage(msg); //핸들러로 메세지 전송
                        } catch (Exception e) {

                        }
                    }
                }
            }).start();
        }
    }

    public void moveMaskY(final int centerY, final double roll, final float cameraHorizontalAngle) {
        synchronized (this) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Iterator iterator = maskList_.keySet().iterator();
                    while (iterator.hasNext()) {
                        int id = (int) iterator.next();
                        MaskItem mask = maskList_.get(id);
                        double roundRoll = Math.round(roll); //y축으로 회전 각 (소수점 다 날려버림) -> Radian 값으로
                        roundRoll = Math.toRadians(roundRoll);
                        try {
                            double increaseHeight = (Math.cos(roundRoll) / Math.cos(Math.toRadians(cameraHorizontalAngle))) * centerY;
                            double newHeight = centerY - increaseHeight;

                            Message msg = handlerY.obtainMessage();
                            msg.arg1 = id;
                            msg.obj = String.valueOf(newHeight);
                            handlerY.sendMessage(msg); //핸들러로 메세지 전송catch
                        } catch (Exception e) {
                        }
                    }
                }
            }).start();
        }
    }

    // 아이템 X축 이동시키는 핸들러
    private Handler handlerX = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int index = msg.arg1;
            float width = Float.valueOf(msg.obj.toString());
            float imageViewW = maskList_.get(index).imageView.getLayoutParams().width/2;
            maskList_.get(index).imageView.setX(width - imageViewW);
        }
    };

    // 아이템 Y축 이동시키는 핸들러
    private Handler handlerY = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int index = msg.arg1;
            float height = Float.valueOf(msg.obj.toString());
            float imageViewH = maskList_.get(index).imageView.getLayoutParams().height/2;
            float floor = maskList_.get(index).floor * imageViewH * 2.1f; //마스크 층에 따른 높이 설정
            maskList_.get(index).imageView.setY(height - imageViewH + floor);
        }
    };

    //사각형 안에 이미지가 있는지 판별하고 결과값으로 색상을 반환
    public @ColorInt
    int printVertex(CanvasRect canvasRect){
        ImageView imageView = maskList_.get(0).imageView;
        //이미지의 가로 세로 크기
        int w = imageView.getWidth();
        int h = imageView.getHeight();

        //사각형의 가로 세로 크기
        float rectWidth = canvasRect.maxX - canvasRect.minX;
        float rectHeight = canvasRect.maxY - canvasRect.minY;
        float errorRange = 1.5f; //사각형과 이미지 오차 범위

        if((canvasRect.minX <= imageView.getX()) && (canvasRect.minY <= imageView.getY())
                && (canvasRect.maxX >= (imageView.getX() + w) && (canvasRect.maxY >= (imageView.getY() + h)))){ //사각형 내부에 이미지가 위치한 경우
            if((rectWidth > (w * errorRange)) || (rectHeight > (h * errorRange))){ //사각형의 가로 or 세로 면이 이미지의 가로 or 세로 면 보다 너무 큰 경우 (오차 범위 초과)
                return Color.argb(255, 255, 120, 0); //오렌지색
            }
            return Color.GREEN;
        }

//        if(rectWidth < w || rectHeight < h){ //사각형의 가로 or 세로 면이 이미지의 가로 or 세로 면 보다 작은 경우
//            return Color.RED;
//        }

//        Log.i("ImageView", "1번 꼭지점 X: " + imageView.getX() + "   Y: " + imageView.getY());
//        Log.i("ImageView", "2번 꼭지점 X: " + (imageView.getX() + w) + "   Y: " + imageView.getY());
//        Log.i("ImageView", "3번 꼭지점 X: " + imageView.getX() + "   Y: " + (imageView.getY() + h));
//        Log.i("ImageView", "4번 꼭지점 X: " + (imageView.getX() + w) + "   Y: " + (imageView.getY() + h));
//
//        Log.i("CanvasRect", "1번 꼭지점 X: " + canvasRect.minX + "   Y: " + canvasRect.minY);
//        Log.i("CanvasRect", "2번 꼭지점 X: " + canvasRect.maxX + "   Y: " + canvasRect.minY);
//        Log.i("CanvasRect", "3번 꼭지점 X: " + canvasRect.minX + "   Y: " + canvasRect.maxY);
//        Log.i("CanvasRect", "4번 꼭지점 X: " + canvasRect.maxX + "   Y: " + canvasRect.maxY);


        return Color.RED;
    }
}
