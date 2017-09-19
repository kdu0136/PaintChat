package com.example.kimdongun.paintchat;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.WindowManager;

import com.google.android.gms.maps.model.Marker;

import static android.hardware.SensorManager.SENSOR_DELAY_GAME;

/**
 * Created by KimDongun on 2017-09-03.
 */

public class SensorARMap implements SensorEventListener {
    private Context mContext;
    private Display mDisplay;

    private final SensorManager mSensorManager; //센서 총괄 메니저
    private final Sensor mAcc; //가속 센서
    private final Sensor mMag; //자기장 센서

    private float azimuthY = 0f; //방위각 (y축 기준)
    public float azimuthZ = 0f; //방위각 (z축 기준)

    private float[] mGravity = new float[3]; //중력 xyz축 저장 (Default 축 => Y축 기준으로 방향 잡기 위함)
    private float[] mGeomagnetic = new float[3]; //자기장 xyz축 저장 (Default 축 => Y축 기준으로 방향 잡기 위함)
    private float[] mGravityZ = new float[3]; //중력 xyz축 저장 (X축기준으로 시계 반대방향 90도 회전한 축 => -Z축 기준으로 방향 잡기 위함)
    private float[] mGeomagneticZ = new float[3]; //자기장 xyz축 저장 (X축기준으로 시계 반대방향 90도 회전한 축 => -Z축 기준으로 방향 잡기 위함)

    private Marker clientMarker; //사용자 마커

    public SensorARMap(Context context){
        this.mContext = context;

        mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); //가속 센서
        mMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD); //자기장 센서


        //화면 크기
        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();
        DebugHandler.log("Disply", "W: " + mDisplay.getWidth() + " H: " + mDisplay.getHeight());
    }

    public void setClientMarker(Marker clientMarker){
        this.clientMarker = clientMarker;
    }

    public void start() {
        mSensorManager.registerListener(this, mAcc, SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMag, SENSOR_DELAY_GAME);
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

            if(mGravity != null && mGeomagnetic != null){
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic); //기본 축(+Y) 기준으로 계산
                if (success) { // +Y축 (단말기 상단을 기준으로 방향 계산 성공)
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

                    if(clientMarker != null)
                        clientMarker.setRotation(azimuthZ);
                }

            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
//		- SENSOR_STATUS_ACCURACY_HIGH : 정확도 높음
//		- SENSOR_STATUS_ACCURACY_MEDIUM : 정확도 보통
//		- SENSOR_STATUS_ACCURACY_LOW : 정확도 낮음
//		- SENSOR_STATUS_UNRELIABLE : 신뢰할 수 없음
    }
}
