package com.example.kimdongun.paintchat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GameCanvas extends View {
    private ArrayList<Path> paths = new ArrayList<Path>(); //이때까지 그린 path 저장
    private ArrayList<Paint> pathPaint = new ArrayList<Paint>(); //이때까지 그린 path의 paint 저장

    private Handler handler_; //캔버스 이벤트 전송하는 핸들러

    public Bitmap canvasBitmap;
    public Canvas canvas;
    public Paint canvasPaint_; //캔버스 패인트

    Bitmap drawBitmap;
    Bitmap drawBitmapResize; //리사이즈 된 비트맵
    Canvas drawCanvas; //pen을 그리기 위한 캔버스
    Paint drawPaint_; //그림 패인트
    Path drawPath_; //그림 path

    String operationType = "";

    private float penWidth_ = 0.01f; //높이가 100일 때 width 1 기준
    private int canvasColor_ = Color.WHITE;

    public boolean drawable_ = true; //자신이 그릴 수 있는 상태
    public boolean isDrawMask_ = false; //마스크 그리기인가

    public GameCanvas(Context context) {
        super(context);
        this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        initPaint();
    }

    public GameCanvas(Context context, @Nullable AttributeSet attrs) {
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
        drawPaint_.setColor(Color.BLACK);
        drawPaint_.setStyle(Paint.Style.STROKE);
        drawPaint_.setStrokeJoin(Paint.Join.ROUND); //모서리 둥근 형태
        drawPaint_.setStrokeCap(Paint.Cap.ROUND); //둥근 모양으로 마무리
        drawPaint_.setStrokeWidth(penWidth_);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvasBitmap != null) {
            if(operationType.equalsIgnoreCase("open")){
                canvas.drawBitmap(canvasBitmap,0, 0, canvasPaint_);
            }
            canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint_);
        }
        if(resizeMode){
            canvas.drawBitmap(drawBitmapResize, 0, 0, canvasPaint_);
        }else {
            canvas.drawBitmap(drawBitmap, 0, 0, canvasPaint_);
        }
        if(drawable_) { //그림 그리는 사람은 pen모드 eraser모드 일때 전부 path표시
            canvas.drawPath(drawPath_, drawPaint_);
        }else{ //그림을 보는 사람은 pen모드 일때만 path표시
            if(!eraserMode_) {
                canvas.drawPath(drawPath_, drawPaint_);
            }
        }
//        canvas.drawBitmap(resize,(getWidth()-resize.getWidth())/2, (getHeight() - resize.getHeight())/2, paint_);
    }

    boolean resizeMode = false;
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (canvasBitmap == null){
            canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(canvasBitmap);
            canvas.drawColor(canvasColor_);

            if(isDrawMask_){
                Bitmap backBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mask_back);
                backBitmap = Bitmap.createScaledBitmap(backBitmap,
                        w, h, false);
                canvas = new Canvas(canvasBitmap);
                canvas.drawColor(Color.WHITE);
                canvas.drawBitmap(backBitmap ,
                        (canvas.getWidth() - backBitmap.getWidth())/2,
                        (canvas.getHeight() - backBitmap.getHeight())/2, canvasPaint_);
                invalidate();
            }
            canvas.save();
        }

        float oldHeight = 0;
        float rate = 1;
        if(drawBitmap == null) {
            DebugHandler.log("Canvas", "Init");
            drawBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            drawBitmapResize = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            drawCanvas = new Canvas(drawBitmap);
            drawCanvas.drawColor(Color.TRANSPARENT);
            drawCanvas.save();
        }else{
            DebugHandler.log("Canvas", "Resize");
//            penWidth_ = penWidth_ / drawCanvas.getHeight();
//            DebugHandler.log("penWidth", "width: " + penWidth_);
            rate = h/(float)canvas.getHeight();
            DebugHandler.log("Canvas", "Rate: " + rate);
            if(rate == 1){ //원래 사이즈로
                resizeMode = false;
                drawBitmap = Bitmap.createScaledBitmap(drawBitmapResize, w, h, false);
                drawCanvas = new Canvas(drawBitmap);
            }else{ //줄어든 사이즈
                resizeMode = true;
                int newWidht = (int)(rate * drawCanvas.getWidth());
                DebugHandler.log("drawCanvas", "newWidth: " + newWidht);
                drawBitmapResize = Bitmap.createScaledBitmap(drawBitmap, newWidht, h, false);
                drawCanvas = new Canvas(drawBitmapResize);
            }
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
            Message msg = handler_.obtainMessage();
            //일반 그리기 모드
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStart(x, y);
                    String[] keys1 = {"type", "motion", "x", "y"};
                    Object[] values1 = {"pen", "start", (double) x, (double) y};
                    String jsonStr1 = JsonEncode.getInstance().encodeCommandJson("gameCanvas", keys1, values1);
                    msg.obj = jsonStr1;
                    handler_.sendMessage(msg);
                    break;

                case MotionEvent.ACTION_MOVE:
                    touchMove(x, y);
                    String[] keys2 = {"type", "motion", "x", "y"};
                    Object[] values2 = {"pen", "move", (double) x, (double) y};
                    String jsonStr2 = JsonEncode.getInstance().encodeCommandJson("gameCanvas", keys2, values2);
                    msg.obj = jsonStr2;
                    handler_.sendMessage(msg);
                    break;

                case MotionEvent.ACTION_UP:
                    touchEnd(x, y);
                    String[] keys3 = {"type", "motion", "x", "y"};
                    Object[] values3 = {"pen", "end", (double) x, (double) y};
                    String jsonStr3 = JsonEncode.getInstance().encodeCommandJson("gameCanvas", keys3, values3);
                    msg.obj = jsonStr3;
                    handler_.sendMessage(msg);
                    break;
            }
        }
        return true;
    }

    public void touchStart(float x, float y){
        //시작 위치를 잡는다
        drawPath_ = new Path();
        drawPath_.moveTo(x * drawCanvas.getWidth(), y* drawCanvas.getHeight());
        invalidate();
    }

    public void touchMove(float x, float y){
        drawPath_.lineTo(x * drawCanvas.getWidth(), y* drawCanvas.getHeight());
        invalidate();
    }

    public void touchEnd(float x, float y){
        drawPath_.lineTo(x * drawCanvas.getWidth(), y* drawCanvas.getHeight());
        drawCanvas.drawPath(drawPath_, drawPaint_);
        paths.add(new Path(drawPath_));
        pathPaint.add(new Paint(drawPaint_));
        drawPath_.reset();
        invalidate();
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
        invalidate();
    }

    boolean eraserMode_ = false; //지우개 모드

    //펜 모드로 전환
    public void setToPen(){
        drawPaint_.setXfermode(null);
//        setPenWidth(penWidth_);
        eraserMode_ = false;
    }

    //지우개 모드로 전환
    public void setToEraser(){
        drawPaint_.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
//        drawPaint_.setStrokeWidth(getPenWidth() * 2f);
        DebugHandler.log("지우개 두깨", drawPaint_.getStrokeWidth() + "");
        eraserMode_ = true;
    }

    //한 단계 되돌리기
    public void restore() {
        //저장된 path가 있으면 되돌리기
        if (paths.size() > 0) {
            drawBitmap.eraseColor(Color.TRANSPARENT);
            paths.remove(paths.size() - 1);
            pathPaint.remove(pathPaint.size() - 1);

            for(int i = 0; i < paths.size(); i++){
                Path path = paths.get(i);
                Paint paint = pathPaint.get(i);
                drawCanvas.drawPath(path, paint);
            }
            invalidate();
        } else {
            Toast.makeText(getContext(), "이전 그림 내용이 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    //화면에 그려진 것 모두 지우기
    public void clear() {
        drawBitmap.eraseColor(Color.TRANSPARENT);
        drawBitmapResize.eraseColor(Color.TRANSPARENT);
        drawCanvas.restore();
        drawCanvas.save();
        paths.clear();
        pathPaint.clear();
        invalidate();
    }

    //펜 색상 변경
    public void setPenColor(int color){
        drawPaint_.setColor(color);
    }

    //펜 두깨
    public int getPenColor(){
        return drawPaint_.getColor();
    }

    public int penLevel_ = 5; //펜 두깨 레벨
    //펜 두깨 증가
    public boolean increasePenWidth(){
        penLevel_++;
        penWidth_ = penWidth_ + 0.002f;
        if(penLevel_ > 15) {
            penLevel_ = 15;
            penWidth_ = 0.03f;
            return false;
        }
        setPenWidth(penWidth_);
        return true;
    }
    //펜 두깨 감소
    public boolean decreasePenWidth(){
        penLevel_--;
        penWidth_ = penWidth_ - 0.002f;
        if(penLevel_ < 1) {
            penLevel_ = 1;
            penWidth_ = 0.002f;
            return false;
        }
        setPenWidth(penWidth_);
        return true;
    }

    //펜 두깨 변경
    public void setPenWidth(float penWidth){
        drawPaint_.setStrokeWidth(penWidth * drawCanvas.getHeight());
        if(drawable_) {
            String[] keys = {"type", "width"};
            Object[] values = {"penWidth", penWidth_};
            String jsonStr = JsonEncode.getInstance().encodeCommandJson("gameCanvas", keys, values);
            Message msg = handler_.obtainMessage();
            msg.obj = jsonStr;
            handler_.sendMessage(msg);
        }
    }

    public float getPenWidth(){
        return drawPaint_.getStrokeWidth();
    }

    //캔버스 색상 변경
    public void setCanvasColor(int color){
        canvas.drawColor(color);
        canvasColor_ = color;
        invalidate();
    }

    //캔버스 두깨
    public int getCanvasColor(){
        return canvasColor_;
    }

    public String saveBitmapOnCache(String file_name, Context context) {
        File storage = context.getCacheDir(); // 어플 캐시 경로
        String fileName = file_name;  // 파일이름
        File cacheFile = new File(storage,fileName);
        try{
            if(!cacheFile.exists())cacheFile.createNewFile();
            FileOutputStream out = new FileOutputStream(cacheFile);
            Bitmap overlayBitmap = Bitmap.createBitmap(canvasBitmap.getWidth(), canvasBitmap.getHeight(), canvasBitmap.getConfig());
            Canvas overlayCanvas = new Canvas(overlayBitmap);
            overlayCanvas.drawBitmap(canvasBitmap, new Matrix(), null); //배경 bitmap 먼저 그리기
            overlayCanvas.drawBitmap(drawBitmap, 0, 0, null); //그림 bitmap 배경 위에 덮어 쓰기
            overlayBitmap.compress(Bitmap.CompressFormat.PNG, 100 , out);  // 넘거 받은 bitmap을 저장해줌
            out.close(); // 마무리로 닫아줍니다.
//            Toast.makeText(context.getApplicationContext(), "파일을 저장 했습니다.", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cacheFile.getAbsolutePath();   // 임시파일 저장경로를 리턴해주면 끝!
    }

    public String saveOnlyPenBitmapOnCache(String file_name, Context context) {
        File storage = context.getCacheDir(); // 어플 캐시 경로
        String fileName = file_name;  // 파일이름
        File cacheFile = new File(storage,fileName);
        try{
            if(!cacheFile.exists())cacheFile.createNewFile();
            FileOutputStream out = new FileOutputStream(cacheFile);
            Bitmap overlayBitmap = Bitmap.createBitmap(drawBitmap.getWidth(), drawBitmap.getHeight(), drawBitmap.getConfig());
            Canvas overlayCanvas = new Canvas(overlayBitmap);
            overlayCanvas.drawBitmap(drawBitmap, new Matrix(), null); //그림 bitmap 그리기
            overlayBitmap.compress(Bitmap.CompressFormat.PNG, 100 , out);  // 넘거 받은 bitmap을 저장해줌
            out.close(); // 마무리로 닫아줍니다.
//            Toast.makeText(context.getApplicationContext(), "파일을 저장 했습니다.", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cacheFile.getAbsolutePath();   // 임시파일 저장경로를 리턴해주면 끝!
    }
}
