package com.example.kimdongun.paintchat;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.WindowManager;

import com.example.kimdongun.paintchat.adapter.MaskManager;
import com.example.kimdongun.paintchat.item.CanvasRect;

import static android.hardware.SensorManager.SENSOR_DELAY_GAME;

/**
 * Created by KimDongun on 2017-09-03.
 */

public class SensorARInventory implements SensorEventListener {
    private Context mContext;
    private Display mDisplay;

    private final SensorManager mSensorManager; //센서 총괄 메니저
    private final Sensor mAcc; //가속 센서
    private final Sensor mMag; //자기장 센서
    private final Sensor mGyro; //자이로스코프 센서

    private float azimuthY = 0f; //방위각 (y축 기준)
    private float azimuthZ = 0f; //방위각 (z축 기준)

    private float[] mGravity = new float[3]; //중력 xyz축 저장 (Default 축 => Y축 기준으로 방향 잡기 위함)
    private float[] mGeomagnetic = new float[3]; //자기장 xyz축 저장 (Default 축 => Y축 기준으로 방향 잡기 위함)
    private float[] mGravityZ = new float[3]; //중력 xyz축 저장 (X축기준으로 시계 반대방향 90도 회전한 축 => -Z축 기준으로 방향 잡기 위함)
    private float[] mGeomagneticZ = new float[3]; //자기장 xyz축 저장 (X축기준으로 시계 반대방향 90도 회전한 축 => -Z축 기준으로 방향 잡기 위함)
    private float[] mGyroscope = new float[3]; //자이로스코프 xyz축 저장

    private double pitch = 0, roll = 0, yaw = 0; //pitch x 기울기 roll y 기울기

    private Handler handler_; //센서 이벤트 전송하는 핸들러
    public boolean isInsideSector = false; //마스크를 범위안에 가둬놨는지 판별
    public CanvasRect canvasRect = new CanvasRect(); //마스크 가둔 사각형
    public MaskManager maskManager; //내 마스크 매니저

    public float cameraHorizontalAngle = 0;
    public float cameraVerticalAngle = 0;

    public void setHandler(Handler handler){
        this.handler_ = handler;
    }

    public SensorARInventory(Context context, boolean myMaskVisible){
        this.mContext = context;
        this.maskManager = new MaskManager(myMaskVisible);

        mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); //가속 센서
        mMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD); //자기장 센서
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE); //자이로스코프 센서

        //화면 크기
        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();
        DebugHandler.log("Disply", "W: " + mDisplay.getWidth() + " H: " + mDisplay.getHeight());
    }

    public void start() {
        mSensorManager.registerListener(this, mAcc, SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMag, SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mGyro, SENSOR_DELAY_GAME);
//		- SENSOR_DELAY_FASTEST : 가장 빠른 속도
//		- SENSOR_DELAY_GAME : 게임에 적합한 속도
//		- SENSOR_DELAY_NORMAL : 화면 방향 전환에 적당한 속도
//		- SENSOR_DELAY_UI : 사용자 조작을 위해 알맞은 속도
    }

    public void stop() {
        mSensorManager.unregisterListener(this);
    }

    //센서의 값이 변경 될 경우
    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            final float alpha = 0.9f; //0~1 : 1에 가까울 수록 오차값 큼 but 부드러움
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) { //가속 센서
//                values[0]: Acceleration minus Gx on the x-axis
//                values[1]: Acceleration minus Gy on the y-axis
//                values[2]: Acceleration minus Gz on the z-axis
                // alpha is calculated as t / (t + dT)
                // with t, the low-pass filter's time-constant
                // and dT, the event delivery rate
                //센서 기준이 +Y축
                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2];

                //Y, Z축 서로 변경 , 센서 기준이 -Z축
                mGravityZ[0] = mGravity[0];
                mGravityZ[1] = -mGravity[2];
                mGravityZ[2] = mGravity[1];
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) { //자기장 센서
                //센서 기준이 +Y축
                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0];
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1];
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2];

                //Y, Z축 서로 변경 , 센서 기준이 -Z축
                mGeomagneticZ[0] = mGeomagnetic[0];
                mGeomagneticZ[1] = -mGeomagnetic[2];
                mGeomagneticZ[2] = mGeomagnetic[1];
            }
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) { //자이로스코프 센서
                mGyroscope[0] = alpha * mGyroscope[0] + (1 - alpha) * event.values[0];
                mGyroscope[1] = alpha * mGyroscope[1] + (1 - alpha) * event.values[1];
                mGyroscope[2] = alpha * mGyroscope[2] + (1 - alpha) * event.values[2];
            }

            if(mGravity != null && mGeomagnetic != null && mGyroscope != null){
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic); //기본 축(+Y) 기준으로 계산
                if (success) { // +Y축 (단말기 상단을 기준으로 방향 계산 성공)
                    complementaty(event.timestamp); //단말기 기울기 구함
                    float orientation[] = new float[3]; // orientation contains: azimut, pitch and roll
                    SensorManager.getOrientation(R, orientation);
                    azimuthY = (float) Math.toDegrees(orientation[0]); // orientation
                    azimuthY = (azimuthY + 360) % 360;
                }

                float R_Z[] = new float[9];
                float I_Z[] = new float[9];
                boolean successZ = SensorManager.getRotationMatrix(R_Z, I_Z, mGravityZ, mGeomagneticZ); //-Z축을 기준으로 계산
                if(successZ) { // -Z축 (단말기 뒷면을 기준으로 방향 계산 성공)
                    float orientation[] = new float[3]; // orientation contains: azimut, pitch and roll
                    SensorManager.getOrientation(R_Z, orientation);
                    azimuthZ = (float) Math.toDegrees(orientation[0]); // orientation
                    azimuthZ = (azimuthZ + 360) % 360;

                    if(maskManager.myMaskVisible && !maskManager.myMaskTouched) { //중심 좌표가 있고, 마스크가 보이는 상태, 마스트 터치 안한 상태
//                        calVirtualBound(Math.round(azimuthZ)); //목표 지점이 시야 안에 있는지 판별
                        //마스크가 시야 안에 있는지 판별
                        maskManager.calVisibleBound(azimuthZ, cameraVerticalAngle * 1.5f);
                        //오브젝트 x 위치 변화 (방위각 이용해서)
                        maskManager.moveMaskX(mDisplay.getWidth()/2, azimuthZ, cameraVerticalAngle);
                        //오브젝트 y 위치 변화 (자이로스코프 이용해서)
                        maskManager.moveMaskY(mDisplay.getHeight()/2, roll, cameraHorizontalAngle);
                    }

                    if(isInsideSector){ //마스크가 범위 안에 있는 경우
                        Message msg = handler_.obtainMessage();
                        msg.arg1 = maskManager.printVertex(canvasRect);
                        handler_.sendMessage(msg);
                    }
                }

            }
        }
    }

    public void setVisibleMyMask(boolean visible){
        maskManager.setVisibleMyMask(visible, azimuthZ); //보이게 시작 했을 때 정면 방위각 고려해서 마스크 보이게
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
//		- SENSOR_STATUS_ACCURACY_HIGH : 정확도 높음
//		- SENSOR_STATUS_ACCURACY_MEDIUM : 정확도 보통
//		- SENSOR_STATUS_ACCURACY_LOW : 정확도 낮음
//		- SENSOR_STATUS_UNRELIABLE : 신뢰할 수 없음
    }

    /**
     * 1차 상보필터 적용 메서드
     * 단말기 기울기 구하는 함수*/
    /*for unsing complementary fliter*/
    private static final float NS2S = 1.0f/1000000000.0f;
    private double timestamp;

    private void complementaty(double new_ts){
        /*센서 값 첫 출력시 dt(=timestamp - event.timestamp)에 오차가 생기므로 처음엔 break */
        if(timestamp == 0){
            timestamp = new_ts;
            return;
        }
        final double dt = (new_ts - timestamp) * NS2S; // ns->s 변환
        timestamp = new_ts;

        /* degree measure for accelerometer */
        double accPitch = -Math.atan2(mGravity[0], mGravity[2]) * 180.0 / Math.PI; // Y 축 기준
        double accRoll= -Math.atan2(mGravity[1], mGravity[2]) * 180.0 / Math.PI; // X 축 기준
        double accYaw= -Math.atan2(mGravity[2], mGravity[0]) * 180.0 / Math.PI; // Z 축 기준

//        roll = 180 * Math.atan (mGravity[1]/Math.sqrt(mGravity[0]*mGravity[0] + mGravity[2]*mGravity[2]))/Math.PI;
//        yaw = 180 * Math.atan (mGravity[2]/Math.sqrt(mGravity[0]*mGravity[0] + mGravity[1]*mGravity[1]))/Math.PI;

        /**
         * 1st complementary filter.
         *  mGyroscope : 각속도 성분.
         *  accPitch : 가속도계를 통해 얻어낸 회전각.
         */
        final float a = 0.2f; //필터 계
        double temp = (1/a) * (accPitch - pitch) + mGyroscope[1];
        pitch = pitch + (temp*dt);

        temp = (1/a) * (accRoll - roll) + mGyroscope[0];
        roll = roll + (temp*dt);

        //가속도 값
        double gSensor = Math.sqrt((mGravity[0]*mGravity[0]) + (mGravity[1]*mGravity[1]) + (mGravity[2]*mGravity[2]));
        yaw = Math.atan2(mGravity[0], mGravity[1]) * 180.0 / Math.PI;
//        temp = (1/a) * (accYaw - yaw) + mGyroscope[2];
//        yaw = yaw + (temp*dt);
    }

//    private void calVirtualBound(float azimuth){
//        //방위각, 현재 위도/경도, 거리를 이용해서 위도/경도 추측하기
//        double distance = 100; //meter (직선 거리)
//        double diameter = 1;//Math.abs(Math.sin(Math.toRadians(roll)));
//        distance = distance * diameter;
//        float cameraAngle = 41; //카메라 화각
////            double distance2 = distance * Math.cos(Math.toRadians(45)); //meter (45도 직선 거리)
////            Log.d("LOCATION", "Cos45: " + Math.cos(Math.toRadians(45)) + "\n직선 거리: " + distance + "\n45도 거리: " + distance2);
//        Coordinate straightCoordinate = new Coordinate();
//        double radiusSquare = CalculateDerivedPosition(centerCoordinate, straightCoordinate, distance, azimuth); //직선 거리 위도, 경도
////        Log.d("LOCATION2", "추측 위치정보 : \n" + straightCoordinate.latitude + ", " + straightCoordinate.longitude +
////                "\n경도 변화량 : " + (straightCoordinate.latitude - centerCoordinate.latitude) +
////                "\n위도 변화량 : " + (straightCoordinate.longitude - centerCoordinate.longitude) +
////                "\n방위각 : " + azimuth + "\n반지름: " + radiusSquare);
//
//        float azimuthP45 = azimuth + cameraAngle; //오른쪽 45도 방위각
//        if(azimuthP45 > 360) azimuthP45 -= 360;  //방위각이 360보다 크게 되면 360 빼줌
//        Coordinate endCoordinate = new Coordinate();
//        double radiusSquare2 = CalculateDerivedPosition(centerCoordinate, endCoordinate, distance, azimuthP45); //오른쪽 화각 끝 위도,경도
////        Log.d("LOCATION2", "추측 위치정보 : \n" + endCoordinate.latitude + ", " + endCoordinate.longitude +
////                "\n경도 변화량 : " + (endCoordinate.latitude - centerCoordinate.latitude) +
////                "\n위도 변화량 : " + (endCoordinate.longitude - centerCoordinate.longitude) +
////                "\n방위각 : " + azimuthP45 + "\n반지름: " + radiusSquare2);
//
//        float azimuthM45 = azimuth - cameraAngle; //왼쪽 45도 방위각
//        if(azimuthM45 < 0) azimuthM45 += 360; //방위각이 음수가 되면 360 더해줌
//        Coordinate startCoordinate = new Coordinate();
//        double radiusSquare3 = CalculateDerivedPosition(centerCoordinate, startCoordinate, distance, azimuthM45); //왼쪽 화각 끝 위도,경도
////        Log.d("LOCATION2", "추측 위치정보 : \n" + startCoordinate.latitude + ", " + startCoordinate.longitude +
////                "\n경도 변화량 : " + (startCoordinate.latitude - centerCoordinate.latitude) +
////                "\n위도 변화량 : " + (startCoordinate.longitude - centerCoordinate.longitude) +
////                "\n방위각 : " + azimuthM45 + "\n반지름: " + radiusSquare3);
//
////        pointCoordinate = new Coordinate(); //37.484014, 126.972387
////        pointCoordinate.latitude = 37.483966;
////        pointCoordinate.longitude = 126.972422;
//
//        isInsideSector = isInsideSector(centerCoordinate, startCoordinate, endCoordinate, pointCoordinate, radiusSquare3);
//        isInsideSector = true;
////        Log.d("LOCATION2", "측정 위치정보 : \n" + pointCoordinate.latitude + ", " + pointCoordinate.longitude +
////                "\n결과 : " + isInsideSector);
//    }
//
//    /// <summary>
//    /// Calculates the end-point from a given source at a given range (meters) and bearing (degrees).
//    /// This methods uses simple geometry equations to calculate the end-point.
//    /// </summary>
//    /// <param name="source">Point of origin</param>
//    /// <param name="range">Range in meters</param>
//    /// <param name="bearing">Bearing in degrees</param>
//    /// <returns>End-point from the source given the desired range and bearing.</returns>
//    public double CalculateDerivedPosition(Coordinate centerCoordinate, Coordinate newCoordinate, double distance, double bearing)
//    {
//        final double EarthRadius = 6378137.0; //meter
//
//        double latitude_old = Math.toRadians(centerCoordinate.latitude);
//        double longitude_old = Math.toRadians(centerCoordinate.longitude);
//        distance = distance / EarthRadius;
//        bearing = Math.toRadians(bearing);
//
//        double newLat = Math.asin( Math.sin(latitude_old) * Math.cos(distance) +
//                Math.cos(latitude_old) * Math.sin(distance) * Math.cos(bearing));
//        double newLon = longitude_old + Math.atan2(Math.sin(bearing) * Math.sin(distance) * Math.cos(latitude_old),
//                Math.cos(distance) - Math.sin(latitude_old) * Math.sin(newLat));
//
//        newCoordinate.latitude = Math.toDegrees(newLat);
//        newCoordinate.longitude = Math.toDegrees(newLon);
//
//        double diffLon = newCoordinate.longitude - centerCoordinate.longitude; //위도 변화량
//        double diffLat = newCoordinate.latitude - centerCoordinate.latitude; //경도 변화량
//
//        double radiusSquared = (diffLon * diffLon) + (diffLat * diffLat); //가상 공간 반지름 제곱
//
////        textView_new_info.setText("추측 위치정보 : \n위도 : " + newCoordinate.longitude + "\n경도 : " + newCoordinate.latitude +
////                "\n위도 변화량 : " + diffLon + "\n경도 변화량 : " + diffLat +
////                "\n방위각 : " + bearing * RadiansToDegrees);
//
//        return radiusSquared;
//    }
//
//    boolean isInsideSector(Coordinate centerCoordinate, Coordinate sectorStartCoordinate,
//                           Coordinate sectorEndCoordinate, Coordinate pointCoordinate, double radiusSquared){
//        Coordinate abPoint = new Coordinate(); //절대 위치 (center를 0,0으로 생각하기 위함)
//        abPoint.latitude = pointCoordinate.latitude - centerCoordinate.latitude;
//        abPoint.longitude = pointCoordinate.longitude - centerCoordinate.longitude;
//
//        Coordinate abStartPoint = new Coordinate(); //절대 위치 (center를 0,0으로 생각하기 위함)
//        abStartPoint.latitude = sectorStartCoordinate.latitude - centerCoordinate.latitude;
//        abStartPoint.longitude = sectorStartCoordinate.longitude - centerCoordinate.longitude;
//
//        Coordinate abEndPoint = new Coordinate(); //절대 위치 (center를 0,0으로 생각하기 위함)
//        abEndPoint.latitude = sectorEndCoordinate.latitude - centerCoordinate.latitude;
//        abEndPoint.longitude = sectorEndCoordinate.longitude - centerCoordinate.longitude;
//
//        return !areClockWise(abStartPoint, abPoint) && areClockWise(abEndPoint, abPoint) && isWithingRadius(abPoint, radiusSquared);
//    }
//
//    boolean isWithingRadius(Coordinate pointCoordinate, double radiusSquared){
//        return ((pointCoordinate.latitude * pointCoordinate.latitude) + (pointCoordinate.longitude * pointCoordinate.longitude)) <= radiusSquared;
//    }
//
//    boolean areClockWise(Coordinate standardCoordinate, Coordinate pointCoordinate){
//        return ((-standardCoordinate.latitude * pointCoordinate.longitude) + (standardCoordinate.longitude * pointCoordinate.latitude)) > 0;
//    }
}
