package com.example.kimdongun.paintchat.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.GameCanvas;
import com.example.kimdongun.paintchat.HandlerMessageDecode;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.dialog.ColorPickerDialog;
import com.example.kimdongun.paintchat.item.ChatRoomListViewItem;
import com.example.kimdongun.paintchat.service.SocketServiceExecutor;
import com.example.kimdongun.paintchat.toast.NormalChatToast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import yuku.ambilwarna.AmbilWarnaDialog;

import static com.example.kimdongun.paintchat.activity.MainActivity.REQ_CODE_SELECT_IMAGE;

public class CanvasActivity extends AppCompatActivity implements View.OnClickListener, HandlerMessageDecode, SocketServiceExecutor, DialogInterface.OnDismissListener {
    private ImageView imageView_arrow_left; //펜 두깨 내리는 이미지
    private ImageView imageView_arrow_right; //펜 두깨 올라는 이미지
    private View imageView_pen_width; //펜 두깨 이미지
    private ImageView imageView_select_pen; //펜 선택 박스
    private ImageView imageView_select_eraser; //지우개 선택 박스
    private ImageView imageView_pen; //펜 이미지
    private ImageView imageView_eraser; //지우개 이미지
    private ImageView imageView_palette; //팔레트 이미지
    private ImageView imageView_restore; //되돌리기 이미지
    private ImageView imageView_clear; //clear 이미지

    private GameCanvas gameCanvas; //게임 그림판
    private Handler canvasHandler_; //그림판 핸들러

    private ImageView imageView_gallery; //사진 배경 선택
    private ImageView imageView_upload; //프로필 업로드

    private Client client_; //클라이언트

    private ColorPickerDialog colorPickerDialog_; //펜 or 캔버스 선택 다이얼 로그

    private NormalChatToast normalChatToast; //채팅 토스트 메세지
    private String from_; //어느 액티비티에서 실행했는지

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void loadImage(String imageFileName, long img, int rot); //이미지 불러오기

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canvas);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //초기화
        client_ = Client.getInstance(this);
        canvasHandler_ = new Handler();

        //이미지 뷰 초기화
        imageView_arrow_left = (ImageView)findViewById(R.id.imageView_arrow_left); //펜 두깨 내리는 이미지
        imageView_arrow_right = (ImageView)findViewById(R.id.imageView_arrow_right); //펜 두깨 올라는 이미지
        imageView_pen_width = (View)findViewById(R.id.imageView_pen_width); //펜 두깨 이미지
        imageView_select_pen = (ImageView)findViewById(R.id.imageView_select_pen); //펜 선택 박스
        imageView_select_eraser = (ImageView)findViewById(R.id.imageView_select_eraser); //지우개 선택 박스
        imageView_pen = (ImageView)findViewById(R.id.imageView_pen); //펜 이미지
        imageView_eraser = (ImageView)findViewById(R.id.imageView_eraser); //지우개 이미지
        imageView_palette = (ImageView)findViewById(R.id.imageView_palette); //팔레트 이미지
        imageView_restore = (ImageView)findViewById(R.id.imageView_restore); //되돌리기 이미지
        imageView_clear = (ImageView)findViewById(R.id.imageView_clear); //clear 이미지
        gameCanvas = (GameCanvas)findViewById(R.id.canvas) ;
        gameCanvas.setHandler(canvasHandler_);

        imageView_gallery = (ImageView)findViewById(R.id.imageView_gallery); //사진 배경 선택
        imageView_upload = (ImageView)findViewById(R.id.imageView_upload); //프로필 업로드

        Intent intent = getIntent();
        from_ = intent.getStringExtra("from");

        gameCanvas.drawable_ = true;
        if(from_.equals("camera"))
            gameCanvas.isDrawMask_ = true;

        normalChatToast = new NormalChatToast(this); //채팅 토스트 메세지
        //터치 이벤트
        imageView_arrow_left.setOnClickListener(this);
        imageView_arrow_right.setOnClickListener(this);
        imageView_pen.setOnClickListener(this);
        imageView_eraser.setOnClickListener(this);
        imageView_palette.setOnClickListener(this);
        imageView_restore.setOnClickListener(this);
        imageView_clear.setOnClickListener(this);
        imageView_gallery.setOnClickListener(this);
        imageView_upload.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home: //홈 버튼 클릭
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        String msg = "";
        if(from_.equals("main")) { //메인화면으로 돌아갈 경우 출력 메세지
            msg = "현재 화면에서 나가면\n프로필 사진이 변경 되지 않습니다.";
        }else if(from_.equals("chat")){ //채팅화면으로 돌아갈 경우 출력 메세지
            msg = "현재 화면에서 나가면\n그리던 그림은 전송 되지 않습니다.\n(그림 파일 저장 안됨)";
        }else if(from_.equals("camera")){ //커스텀 카메라 화면으로 돌아갈 경우 출력 메세지
            msg = "현재 화면에서 나가면\n커스텀 마스크는 저장 되지 않습니다.\n(그림 파일 저장 안됨)";
        }
        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(CanvasActivity.this);
        alert_confirm.setMessage(msg).setCancelable(false).setPositiveButton("나가기",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //나가기
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("filePath", "");
                        setResult(Activity.RESULT_OK,returnIntent);
                        finish();
                    }
                }).setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
        AlertDialog alert = alert_confirm.create();
        alert.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.imageView_arrow_left: //펜 두깨 내림
                if(gameCanvas.decreasePenWidth()) {
                    DebugHandler.log(getClass().getName(), "펜 두깨 내림");
                    ViewGroup.LayoutParams pen_width_params = imageView_pen_width.getLayoutParams();
                    pen_width_params.height--;
                    imageView_pen_width.setLayoutParams(pen_width_params);
                    DebugHandler.log(getClass().getName(), "펜 UI 두깨: " +  pen_width_params.height);
                }
                break;

            case R.id.imageView_arrow_right: //펜 두깨 올림
                if(gameCanvas.increasePenWidth()) {
                    DebugHandler.log(getClass().getName(), "펜 두깨 올림");
                    ViewGroup.LayoutParams pen_width_params = imageView_pen_width.getLayoutParams();
                    pen_width_params.height++;
                    imageView_pen_width.setLayoutParams(pen_width_params);
                    DebugHandler.log(getClass().getName(), "펜 UI 두깨: " +  pen_width_params.height);
                }
                break;

            case R.id.imageView_pen: //펜 선택
                imageView_select_pen.setVisibility(View.VISIBLE);
                imageView_select_eraser.setVisibility(View.INVISIBLE);
                gameCanvas.setToPen();
                imageView_pen_width.setBackgroundColor(gameCanvas.getPenColor()); //선 두깨 색상 펜 색상으로
                break;

            case R.id.imageView_eraser: //지우개 선택
                imageView_select_pen.setVisibility(View.INVISIBLE);
                imageView_select_eraser.setVisibility(View.VISIBLE);
                gameCanvas.setToEraser();
                imageView_pen_width.setBackgroundColor(Color.BLACK); //선 두깨 색상 검정색으로
                break;

            case R.id.imageView_palette: //팔레트 선택
                colorPickerDialog_ = new ColorPickerDialog(this);
                colorPickerDialog_.setOnDismissListener(this);
                colorPickerDialog_.show();
                break;

            case R.id.imageView_restore: //되돌리기 선택
                gameCanvas.restore();
                break;

            case R.id.imageView_clear: //clear 선택
                gameCanvas.clear();
                //펜 선택으로 변경
                imageView_select_pen.setVisibility(View.VISIBLE);
                imageView_select_eraser.setVisibility(View.INVISIBLE);
                gameCanvas.setToPen();
                imageView_pen_width.setBackgroundColor(gameCanvas.getPenColor()); //선 두깨 색상 펜 색상으로
                break;

            case R.id.imageView_gallery: //사진 배경 선택
                if(!from_.equals("camera")) {
                    Intent intent1 = new Intent(Intent.ACTION_PICK);
                    intent1.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
                    intent1.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent1, REQ_CODE_SELECT_IMAGE);
                }
                break;

            case R.id.imageView_upload: //사진 업로드:
                String msg = "";
                if(from_.equals("main")) { //메인화면으로 돌아갈 경우 출력 메세지
                    msg = "프로필 사진을 변경 하시겠습니까?";
                }else if(from_.equals("chat")){ //채팅화면으로 돌아갈 경우 출력 메세지
                    msg = "그린 그림을 전송 하시겠습니까?";
                }else if(from_.equals("camera")){ //커스텀 카메라 화면으로 돌아갈 경우 출력 메세지
                    msg = "그린 마스크를 저장 하시겠습니까?";
                }
                AlertDialog.Builder alert_confirm = new AlertDialog.Builder(CanvasActivity.this);
                alert_confirm.setMessage(msg).setCancelable(false).setPositiveButton("확인",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //그림 파일 캐시에 저장
                                String fileName = client_.account_.id_ + ".png";
                                String filePath = "";
                                if(from_.equals("camera")){
                                    filePath = gameCanvas.saveOnlyPenBitmapOnCache(fileName, CanvasActivity.this); //pen만 저장
                                }else{
                                    filePath = gameCanvas.saveBitmapOnCache(fileName, CanvasActivity.this); //캔버스 배경과 pen을 같이 저장
                                }

                                Intent returnIntent = getIntent();
                                returnIntent.putExtra("filePath", filePath);
                                setResult(Activity.RESULT_OK,returnIntent);
                                finish();
                            }
                        }).setNegativeButton("취소",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                            }
                        });
                AlertDialog alert = alert_confirm.create();
                alert.show();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        DebugHandler.log(getClass().getName(), "requestCode: " + requestCode);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQ_CODE_SELECT_IMAGE) {
                final Uri galleryImgUri = data.getData();
                final String imagePath = getRealPathFromURI(galleryImgUri); // path 경로

                AlertDialog.Builder alert_confirm = new AlertDialog.Builder(CanvasActivity.this);
                alert_confirm.setMessage("사진 크기를 배경에 맞게 조절 하시겠습니까?").setCancelable(false).setPositiveButton("조절",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                //이미지 불러오기
//                                Mat img_input = new Mat();
//                                int rot = ImageManager.calRotate(imagePath);
//                                loadImage(imagePath, img_input.getNativeObjAddr(), rot);
//                                Bitmap resource = Bitmap.createBitmap(img_input.cols(), img_input.rows(), Bitmap.Config.ARGB_8888);
//                                Utils.matToBitmap(img_input, resource);
//                                //이미지 회전
//                                resource = ImageManager.imageRotate(imagePath, resource);
//
//                                resource = Bitmap.createScaledBitmap(resource,
//                                        gameCanvas.canvas.getWidth(), gameCanvas.canvas.getHeight(), false);
//
//                                gameCanvas.canvas = new Canvas(gameCanvas.canvasBitmap);
//                                gameCanvas.canvas.drawColor(Color.WHITE);
//                                gameCanvas.canvas.drawBitmap(resource ,
//                                        (gameCanvas.canvas.getWidth() - resource.getWidth())/2,
//                                        (gameCanvas.canvas.getHeight() - resource.getHeight())/2, gameCanvas.canvasPaint_);
//                                gameCanvas.invalidate();
                                Glide.with(CanvasActivity.this)
                                        .load(imagePath)    // you can pass url too
                                        .asBitmap()
                                        .into(new SimpleTarget<Bitmap>() {
                                            @Override
                                            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                                // you can do something with loaded bitmap her
                                                resource = Bitmap.createScaledBitmap(resource,
                                                        gameCanvas.canvasBitmap.getWidth(), gameCanvas.canvasBitmap.getHeight(), false);

                                                gameCanvas.canvas = new Canvas(gameCanvas.canvasBitmap);
                                                gameCanvas.canvas.drawColor(Color.WHITE);
                                                gameCanvas.canvas.drawBitmap(resource ,
                                                        (gameCanvas.canvas.getWidth() - resource.getWidth())/2,
                                                        (gameCanvas.canvas.getHeight() - resource.getHeight())/2, gameCanvas.canvasPaint_);
                                                gameCanvas.invalidate();
                                            }
                                        });

                            }
                        }).setNegativeButton("아니오",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                Mat img_input = new Mat();
//                                //이미지 불러오기
//                                int rot = ImageManager.calRotate(imagePath);
//                                loadImage(imagePath, img_input.getNativeObjAddr(), rot);
//                                img_input = ImageManager.imageRotate(imagePath, img_input);
//                                Bitmap resource = Bitmap.createBitmap(img_input.cols(), img_input.rows(), Bitmap.Config.ARGB_8888);
//                                Utils.matToBitmap(img_input, resource);
//                                //이미지 회전
//                                resource = ImageManager.imageRotate(imagePath, resource);
//
//                                // you can do something with loaded bitmap her
//                                int width = resource.getWidth();
//                                int height = resource.getHeight();
//                                if(gameCanvas.canvasBitmap.getWidth() < resource.getHeight()){
//                                    width = gameCanvas.canvasBitmap.getWidth();
//                                }
//                                if(gameCanvas.canvasBitmap.getHeight() < resource.getHeight()){
//                                    height = gameCanvas.canvasBitmap.getHeight();
//                                }
//                                resource = Bitmap.createScaledBitmap(resource,
//                                        width, height, false);
//
//                                gameCanvas.canvas = new Canvas(gameCanvas.canvasBitmap);
//                                gameCanvas.canvas.drawColor(Color.WHITE);
//                                gameCanvas.canvas.drawBitmap(resource ,
//                                        (gameCanvas.canvas.getWidth() - resource.getWidth())/2,
//                                        (gameCanvas.canvas.getHeight() - resource.getHeight())/2, gameCanvas.canvasPaint_);
//                                gameCanvas.invalidate();

                                Glide.with(CanvasActivity.this)
                                        .load(imagePath)    // you can pass url too
                                        .asBitmap()
                                        .into(new SimpleTarget<Bitmap>() {
                                            @Override
                                            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                                // you can do something with loaded bitmap her
                                                int width = resource.getWidth();
                                                int height = resource.getHeight();
                                                if(gameCanvas.canvasBitmap.getWidth() < resource.getHeight()){
                                                    width = gameCanvas.canvasBitmap.getWidth();
                                                }
                                                if(gameCanvas.canvasBitmap.getHeight() < resource.getHeight()){
                                                    height = gameCanvas.canvasBitmap.getHeight();
                                                }
                                                resource = Bitmap.createScaledBitmap(resource,
                                                        width, height, false);

                                                gameCanvas.canvas = new Canvas(gameCanvas.canvasBitmap);
                                                gameCanvas.canvas.drawColor(Color.WHITE);
                                                gameCanvas.canvas.drawBitmap(resource ,
                                                        (gameCanvas.canvas.getWidth() - resource.getWidth())/2,
                                                        (gameCanvas.canvas.getHeight() - resource.getHeight())/2, gameCanvas.canvasPaint_);
                                                gameCanvas.invalidate();
                                            }
                                        });
                            }
                        });
                AlertDialog alert = alert_confirm.create();
                alert.show();
            }
        }
    }
    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    public void doReceiveAction(String request){
        DebugHandler.log(getClass().getName() + " JSON", request);
        ArrayList<Object> arrayList = new ArrayList<Object>();
        try {
            JSONObject json = new JSONObject(request);
            String cmdStr = (String)json.get("cmd"); //명령 커맨드 값
            Object requestJson = json.get("request"); //명령 디테일 값

            arrayList.add(cmdStr); //명령 커맨드
            arrayList.add(requestJson); //명령 디테일
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String cmd = (String)arrayList.get(0); //커맨드 값
        final Object requestObj = arrayList.get(1); //명령 디테일 값

        switch (cmd){
            case "normalChat": //채팅 행동
                receiveNormalChat(requestObj);
                break;

            case "readChat": //채팅 읽음 처리 행동
                receiveReadChat(requestObj); //서비스에서 처리할 것 (SQLite에 해당 message 읽은 수 줄임)
                break;

            case "exitServerResult": //서버 접속 종료 행동
                receiveExitServerResult();
                break;
        }
    }

    @Override
    public void receiveNormalChat(Object jsonObj) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("roomKey", json.get("roomKey")); //채팅방 키값
            map.put("id", json.get("id")); //채팅 메세지 보낸 유저 아이디
            map.put("nick", json.get("nick")); //채팅 메세지 보낸 유저 닉네임
            map.put("msg", json.get("msg")); //채팅 메세지
            map.put("date", json.get("date")); //채팅 메세지 보낸 시간
            map.put("num", json.get("num")); //채팅 메세지 읽음 수
            map.put("name", json.get("name")); //채팅 방 이름
            map.put("type", json.get("type")); //타입  ex)chat / image / video / invite
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Date date = new Date((long)map.get("date"));
        SimpleDateFormat sdfTime = new SimpleDateFormat("aa hh:mm");
        String strTime = sdfTime.format(date);

        ChatRoomListViewItem chatRoomListViewItem = client_.chatRoomListViewAdapter.getItem((String)map.get("roomKey"));
        if(chatRoomListViewItem == null){ //받은 메세지의 방 키값을 가진 방이 없는 경우 (채팅방 리스트에 해당 채팅 방 추가)
            int user_num = (int)map.get("num"); //채팅방있는 유저 수
            user_num += 1; //본인 추가

            String[] accountNickArray = ((String)map.get("name")).split(",");
            //방 리스트에 뜨는 친구 닉네임 설정 (방 제목에서 자신의 닉네임은 안 보이도록)
            ArrayList<String> tempList = new ArrayList<>();
            //닉네임 중 자신을 지우기
            for(int i = 0; i < accountNickArray.length; i++) {
                if(!accountNickArray[i].equals(client_.account_.nick_)) {
                    DebugHandler.log(getClass().getName(),"accountNickArray: " + accountNickArray[i]);
                    tempList.add(accountNickArray[i]);
                }
            }
            //방 리스트에 뜨는 친구 닉네임 설정 (방 제목)
            //프로필 사진 url가져오기
            String newRoomName = "";
            final ArrayList<String> profileList = new ArrayList<>();
            for(int i = 0; i < tempList.size(); i++){
                Account account = client_.socialListViewAdapter.getItemByNick(tempList.get(i));
                if(account == null){
                    account = client_.recommendSocialListViewAdapter.getItemByNick(tempList.get(i));
                }
                if(account == null){
                    account = new Account("", "http://211.110.229.53/profile_image/default.png", null, tempList.get(i),
                            null, 1, 1, 1);
                }
                profileList.add(account.profileUrl_);
                newRoomName += tempList.get(i);
                if (i < tempList.size() - 1)
                    newRoomName += ",";
            }

            chatRoomListViewItem = new ChatRoomListViewItem((String)map.get("roomKey"), profileList, newRoomName, (int)user_num,
                    (String)map.get("msg"), strTime, 1);

        }else{ //받은 메세지의 방 키값을 가진 방이 있는 경우 (채팅방 리스트에 있는 아이템 수정)
            //채팅방 리스트뷰에 보이는 정보 수정 (마지막 메세지, 날짜, 안 읽은 메세지 수)
            chatRoomListViewItem.msgNum_ += 1;
            chatRoomListViewItem.msg_ = (String)map.get("msg");
            chatRoomListViewItem.time_ = strTime;
            client_.chatRoomListViewAdapter.removeItem(chatRoomListViewItem.roomKey_);
        }
        chatRoomListViewItem.type_ = (String)map.get("type");
        client_.chatRoomListViewAdapter.addTopItem(chatRoomListViewItem);
        client_.chatRoomListViewAdapter.notifyDataSetChanged();

        Account account = client_.socialListViewAdapter.getItemById((String)map.get("id"));
        if(account == null){
            account = client_.recommendSocialListViewAdapter.getItemById((String)map.get("id"));
        }
        if(account == null){
            account = new Account((String)map.get("id"), "http://211.110.229.53/profile_image/default.png", null, (String)map.get("nick"),
                    null, 1, 1, 1);
        }

        normalChatToast.showToast(account.profileUrl_, account.nick_, (String)map.get("msg"), (String)map.get("type"));
    }

    @Override
    public void receiveReadChat(Object jsonObj) {
    }

    @Override
    public void receiveExitServerResult() {
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if(dialog.equals(colorPickerDialog_)){
            //펜 선택으로 변경
            imageView_select_pen.setVisibility(View.VISIBLE);
            imageView_select_eraser.setVisibility(View.INVISIBLE);
            gameCanvas.setToPen();
            imageView_pen_width.setBackgroundColor(gameCanvas.getPenColor()); //선 두깨 색상 펜 색상으로

            if(colorPickerDialog_.choice.equals("pen")){
                // initialColor is the initially-selected color to be shown in the rectangle on the left of the arrow.
                // for example, 0xff000000 is black, 0xff0000ff is blue. Please be aware of the initial 0xff which is the alpha.
                AmbilWarnaDialog colorPickerDialog = new AmbilWarnaDialog(this, gameCanvas.getPenColor(), new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        // color is the color selected by the user.
                        gameCanvas.setPenColor(color);
                        imageView_pen_width.setBackgroundColor(color);
                    }

                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                colorPickerDialog.show();
            }else if(colorPickerDialog_.choice.equals("canvas")){
                // initialColor is the initially-selected color to be shown in the rectangle on the left of the arrow.
                // for example, 0xff000000 is black, 0xff0000ff is blue. Please be aware of the initial 0xff which is the alpha.
                AmbilWarnaDialog colorPickerDialog = new AmbilWarnaDialog(this, gameCanvas.getCanvasColor(), new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        // color is the color selected by the user.
                        if(!from_.equals("camera")) {
                            gameCanvas.setCanvasColor(color);
                        }
                    }

                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                colorPickerDialog.show();
            }
        }
    }
}
