package com.example.kimdongun.paintchat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.kimdongun.paintchat.item.CanvasRect;

public class MaskGameCanvas extends View {
    Bitmap drawBitmap;
    Canvas drawCanvas; //pen을 그리기 위한 캔버스
    Paint drawPaint_; //그림 패인트
    Paint rectPaint_; //사각형 패인트
    Paint canvasPaint_; //캔버스 패인트
    Path drawPath_; //그림 path
    private CanvasRect canvasRect; //사각형 범위

    private Handler handler_; //캔버스 이벤트 전송하는 핸들러
    private float penWidth_ = 0.01f; //높이가 100일 때 width 1 기준
    private int initRectColor = Color.YELLOW;

    public boolean drawable_ = true; //그릴 수 있는 여부

    public MaskGameCanvas(Context context) {
        super(context);
        this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        initPaint();
    }

    public MaskGameCanvas(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        initPaint();
    }
    public void setHandler(Handler handler){
        this.handler_ = handler;
    }

    //페인트 초기화
    public void initPaint(){
        drawPath_ = new Path();
        //그림 패인트
        drawPaint_ = new Paint();
        drawPaint_.setAntiAlias(true); //좀 더 부드럽게 보여줌
        drawPaint_.setDither(true);
        drawPaint_.setColor(initRectColor);
        drawPaint_.setStyle(Paint.Style.STROKE);
        drawPaint_.setStrokeJoin(Paint.Join.ROUND); //모서리 둥근 형태
        drawPaint_.setStrokeCap(Paint.Cap.ROUND); //둥근 모양으로 마무리
        drawPaint_.setStrokeWidth(penWidth_);
        //사각형 패인트
        rectPaint_ = new Paint();
        rectPaint_.setAntiAlias(true); //좀 더 부드럽게 보여줌
        rectPaint_.setDither(true);
        rectPaint_.setColor(initRectColor);
        rectPaint_.setStyle(Paint.Style.STROKE);
        rectPaint_.setStrokeJoin(Paint.Join.ROUND); //모서리 둥근 형태
        rectPaint_.setStrokeCap(Paint.Cap.ROUND); //둥근 모양으로 마무리
        rectPaint_.setStrokeWidth(penWidth_);

        canvasRect = new CanvasRect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(drawBitmap, 0, 0, canvasPaint_);

        canvas.drawPath(drawPath_, drawPaint_);
        canvas.drawRect(canvasRect.minX, canvasRect.minY, canvasRect.maxX, canvasRect.maxY, rectPaint_);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if(drawBitmap == null) {
            DebugHandler.log("Canvas", "Init");
            drawBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            drawCanvas = new Canvas(drawBitmap);
            drawCanvas.drawColor(Color.TRANSPARENT);
            drawCanvas.save();
        }

        setPenWidth(penWidth_);
        DebugHandler.log("drawCanvas", "width: " + drawCanvas.getWidth() + " height: " + drawCanvas.getHeight());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(drawable_) {
            final float x = event.getX() / drawCanvas.getWidth();
            final float y = event.getY() / drawCanvas.getHeight();
            //일반 그리기 모드
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStart(x, y);
                    break;

                case MotionEvent.ACTION_MOVE:
                    touchMove(x, y);
                    break;

                case MotionEvent.ACTION_UP:
                    touchEnd(x, y);
                    break;
            }
        }
        return true;
    }
    public void touchStart(float x, float y){
        rectPaint_.setColor(initRectColor); //사각형 색상 초기화
        //시작 위치를 잡는다
        drawPath_ = new Path();
        drawPath_.moveTo(x * drawCanvas.getWidth(), y* drawCanvas.getHeight());
        invalidate();

        canvasRect.minX = drawCanvas.getWidth();
        canvasRect.maxX = 0;
        canvasRect.minY = drawCanvas.getHeight();
        canvasRect.maxY = 0;
    }

    public void touchMove(float x, float y){
        drawPath_.lineTo(x * drawCanvas.getWidth(), y * drawCanvas.getHeight());
        invalidate();

        canvasRect.minX = min(canvasRect.minX, x * drawCanvas.getWidth());
        canvasRect.maxX = max(canvasRect.maxX, x * drawCanvas.getWidth());
        canvasRect.minY = min(canvasRect.minY, y * drawCanvas.getHeight());
        canvasRect.maxY = max(canvasRect.maxY, y * drawCanvas.getHeight());
    }

    public void touchEnd(float x, float y){
        drawPath_.lineTo(x * drawCanvas.getWidth(), y * drawCanvas.getHeight());

        canvasRect.minX = min(canvasRect.minX, x * drawCanvas.getWidth());
        canvasRect.maxX = max(canvasRect.maxX, x * drawCanvas.getWidth());
        canvasRect.minY = min(canvasRect.minY, y * drawCanvas.getHeight());
        canvasRect.maxY = max(canvasRect.maxY, y * drawCanvas.getHeight());

        Message msg = handler_.obtainMessage();
        msg.obj = canvasRect;
        handler_.sendMessage(msg);
        //float left, float top, float right, float bottom, @NonNull Paint paint

        drawPath_.reset();
    }

    public void drawRect(@ColorInt int color){
        rectPaint_.setColor(color);
        invalidate();
    }

    private float min(float oldValue, float newValue){
        return oldValue > newValue ? newValue:oldValue; //더 작은 값을 리턴
    }
    private float max(float oldValue, float newValue){
        return oldValue > newValue ? oldValue:newValue; //더 큰 값을 리턴
    }

    //펜 두깨 변경
    public void setPenWidth(float penWidth){
        drawPaint_.setStrokeWidth(penWidth * drawCanvas.getHeight());
        rectPaint_.setStrokeWidth(penWidth * drawCanvas.getHeight());
    }
}
