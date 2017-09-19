package com.example.kimdongun.paintchat.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.CustomBitmapPool;
import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.JsonEncode;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.SQLiteHandler;
import com.example.kimdongun.paintchat.activity.ImageFilterActivity;
import com.example.kimdongun.paintchat.activity.ImageFullActivity;
import com.example.kimdongun.paintchat.activity.LoginActivity;
import com.example.kimdongun.paintchat.activity.MainActivity;
import com.example.kimdongun.paintchat.activity.MapsActivity;
import com.example.kimdongun.paintchat.dialog.ImagePickerDialog;
import com.example.kimdongun.paintchat.network.Network;
import com.example.kimdongun.paintchat.server.OnFinishDBListener;
import com.example.kimdongun.paintchat.server.PostDB;

import org.opencv.core.Mat;

import java.io.File;
import java.util.HashMap;

import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.CropCircleTransformation;

import static com.example.kimdongun.paintchat.activity.MainActivity.REQ_CODE_PROFILE_FILTER;
import static com.example.kimdongun.paintchat.activity.MainActivity.REQ_CODE_SELECT_CAMERA;
import static com.example.kimdongun.paintchat.activity.MainActivity.REQ_CODE_SELECT_CANVAS;
import static com.example.kimdongun.paintchat.activity.MainActivity.REQ_CODE_SELECT_IMAGE;

/**
 * Created by KimDongun on 2016-12-30.
 */

public class FragmentAccount extends Fragment implements View.OnClickListener{
    private final String PROFILE_PATH = "http://211.110.229.53/profile_image/%s";
    private Account account_; //계정 정보
    private ImageView imageView_back; //배경 사진
    private ImageView imageView_profile; //프로필 사진
    private TextView textView_id; //아이디
    private TextView textView_email; //이메일
    private TextView textView_type; //계정 유형 ex)Google, Facebook, Local
    private EditText editText_nick; //닉네임
    private Button button_nick; //닉네임 수정 버튼
    private Switch switch_search; //검색 설정 스위치
    private TextView textView_search; //검색 설정 텍스트
    private Switch switch_push; //푸시 설정 스위치
    private TextView textView_push; //푸시 설정 텍스트

    private ImageView imageView_profile_change; //프로필 변경 버튼

    private ImagePickerDialog imagePickerDialog_; //프로필 선택 다이얼로그

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void loadImage(String imageFileName, long img, int resizing); //이미지 불러오기
    public native void saveImage(String saveFilePath, String saveFileName, long maskImg); //이미지 저장

    public FragmentAccount() {
        // Required empty public constructor
    }
    @Override
    public String toString() {
        return "FragmentAccount";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_account, container, false);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        //초기화
        account_ = ((MainActivity)getContext()).client_.account_;
        imageView_back = (ImageView)view.findViewById(R.id.imageView_back);
        imageView_profile = (ImageView)view.findViewById(R.id.imageView_profile);
        textView_id = (TextView)view.findViewById(R.id.textView_id);
        textView_email = (TextView)view.findViewById(R.id.textView_email);
        textView_type = (TextView)view.findViewById(R.id.textView_type);
        editText_nick = (EditText)view.findViewById(R.id.editText_nick);
        button_nick = (Button)view.findViewById(R.id.button_nick);

        switch_search = (Switch)view.findViewById(R.id.switch_search);
        textView_search = (TextView)view.findViewById(R.id.textView_search);
        switch_push = (Switch)view.findViewById(R.id.switch_push);
        textView_push = (TextView)view.findViewById(R.id.textView_push);

        imageView_profile_change = (ImageView)view.findViewById(R.id.imageView_profile_change);

        //배경 이미지 꽉 차게 설정
        imageView_back.setScaleType(ImageView.ScaleType.FIT_XY);
        //프로필 사진 창 크기 변경
        imageView_profile.getLayoutParams().height = ((MainActivity) getContext()).getWindowManager().getDefaultDisplay().getWidth() / 4;
        imageView_profile.getLayoutParams().width = imageView_profile.getLayoutParams().height;

        Glide.with(this)
                .load(account_.profileUrl_ + "Thumb.png")
                .thumbnail(0.1f)
                .error(R.mipmap.ic_launcher)
                .centerCrop()
                .bitmapTransform(new CropCircleTransformation(new CustomBitmapPool()))
                .into(imageView_profile);

        Glide.with(this)
                .load(account_.profileUrl_ + "Thumb.png")
                .thumbnail(0.1f)
                .error(R.mipmap.ic_launcher)
                .bitmapTransform(new BlurTransformation(getContext()))
                .into(imageView_back);

        textView_id.setText(account_.id_);
        textView_email.setText(account_.email_);
        textView_type.setText(account_.type_);
        editText_nick.setText(account_.nick_);

        int setting_search = account_.search_;
        if(setting_search == 1)
            switch_search.setChecked(true);
        else
            switch_search.setChecked(false);

        if(setting_search == 1){
            textView_search.setText("설정");
        }else{
            textView_search.setText("해제");
        }

        int setting_push = account_.push_;
        if(setting_push == 1)
            switch_push.setChecked(true);
        else
            switch_push.setChecked(false);

        if(setting_push == 1){
            textView_push.setText("설정");
        }else{
            textView_push.setText("해제");
        }

        //푸시 허용 스위치를 눌렀을때 이벤트
        switch_push.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                String netState = Network.getWhatKindOfNetwork(getActivity());
                if(netState.equals(Network.NONE_STATE)){
                    Network.connectNetwork(getActivity());
                    switch_push.setChecked(!isChecked);
                }else {
                    //yes
                    if(isChecked){
                        account_.push_ = 1;
                    }else{
                        account_.push_ = 0;
                    }

                    HashMap<String, String> postData = new HashMap<String, String>();
                    postData.put("id", String.valueOf(account_.id_));
                    postData.put("columnName", "push");
                    postData.put("columnValue", String.valueOf(account_.push_));

                    PostDB postDB = new PostDB();
                    postDB.putFileData("update_account.php", postData, null, null, new OnFinishDBListener() {
                        @Override
                        public void onSuccess(String output) {
                            Toast.makeText(getContext(),"수정되었습니다.",Toast.LENGTH_SHORT).show();
                            String[] columnNames = {"push"};
                            Object[] columnValues = {account_.push_};
                            String[] whereNames = {"id"};
                            Object[] whereValues = {account_.id_};
                            SQLiteHandler handler = new SQLiteHandler(getContext(), "project_one", null, 1);
                            handler.update("account_info", columnNames, columnValues, whereNames, whereValues, "and");
                            if(isChecked) {
                                textView_push.setText("설정");
                            }else{
                                textView_push.setText("해제");
                            }
                        }
                    });
                }
            }
        });

        //검색 허용 스위치를 눌렀을때 이벤트
        switch_search.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                String netState = Network.getWhatKindOfNetwork(getActivity());
                if(netState.equals(Network.NONE_STATE)){
                    Network.connectNetwork(getActivity());
                    switch_search.setChecked(!isChecked);
                }else {
                    //yes
                    if(isChecked){
                        account_.search_ = 1;
                    }else{
                        account_.search_ = 0;
                    }

                    HashMap<String, String> postData = new HashMap<String, String>();
                    postData.put("id", String.valueOf(account_.id_));
                    postData.put("columnName", "search");
                    postData.put("columnValue", String.valueOf(account_.search_));

                    PostDB postDB = new PostDB();
                    postDB.putFileData("update_account.php", postData, null, null, new OnFinishDBListener() {
                        @Override
                        public void onSuccess(String output) {
                            Toast.makeText(getContext(),"수정되었습니다.",Toast.LENGTH_SHORT).show();
                            String[] columnNames = {"search"};
                            Object[] columnValues = {account_.search_};
                            String[] whereNames = {"id"};
                            Object[] whereValues = {account_.id_};
                            SQLiteHandler handler = new SQLiteHandler(getContext(), "project_one", null, 1);
                            handler.update("account_info", columnNames, columnValues, whereNames, whereValues, "and");
                            if(isChecked){
                                textView_search.setText("설정");
                            }else{
                                textView_search.setText("해제");
                            }
                        }
                    });
                }
            }
        });

        //터치 이벤트
        imageView_profile.setOnClickListener(this);
        button_nick.setOnClickListener(this);
        imageView_profile_change.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(getActivity());
        switch (v.getId()){
            case R.id.imageView_profile: //프로필 사진
                Intent intent0 = new Intent(getContext(), ImageFullActivity.class);
                intent0.putExtra("url", account_.profileUrl_);
                intent0.putExtra("title", "프로필");
                startActivity(intent0);
                break;

            case R.id.button_nick: //닉네임 수정 버튼
                if(editText_nick.getText().toString().getBytes().length <= 0){
                    Toast.makeText(getActivity(), "닉네임을 입력하세요", Toast.LENGTH_SHORT).show();
                    break;
                }
                alert_confirm.setMessage("'"+editText_nick.getText().toString() + "'(으)로 닉네임을 수정 하시겠습니까?").setCancelable(false).setPositiveButton("확인",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String netState = Network.getWhatKindOfNetwork(getActivity());
                                if(netState.equals(Network.NONE_STATE)){
                                    Network.connectNetwork(getActivity());
                                }else {
                                    //yes
                                    HashMap<String, String> postData = new HashMap<String, String>();
                                    postData.put("id", String.valueOf(account_.id_));
                                    postData.put("columnName", "nick");
                                    postData.put("columnValue", editText_nick.getText().toString());

                                    PostDB postDB = new PostDB();
                                    postDB.putFileData("update_account.php", postData, null, null, new OnFinishDBListener() {
                                        @Override
                                        public void onSuccess(String output) {
                                            if(output.equals("exist")){ //이미 존재하는 닉네임
                                                Toast.makeText(getContext(), editText_nick.getText().toString() + " 은(는) 이미 존재하는 닉네임 입니다.",Toast.LENGTH_SHORT).show();
                                                editText_nick.setText(account_.nick_);
                                            }else{
                                                Toast.makeText(getContext(),"수정되었습니다.",Toast.LENGTH_SHORT).show();
                                                account_.nick_ = editText_nick.getText().toString();
                                                String[] columnNames = {"nick"};
                                                Object[] columnValues = {account_.nick_};
                                                String[] whereNames = {"id"};
                                                Object[] whereValues = {account_.id_};
                                                SQLiteHandler handler = new SQLiteHandler(getContext(), "project_one", null, 1);
                                                handler.update("account_info", columnNames, columnValues, whereNames, whereValues, "and");

                                                //소켓 서버로 업데이트 된 닉네임 전송
                                                String[] keys = {"nick"};
                                                Object[] values = {account_.nick_};
                                                String jsonStr = JsonEncode.getInstance().encodeCommandJson("nickChange", keys, values);
                                                //서버로 전송
                                                if(((MainActivity)getActivity()).isLiveBinder)
                                                    ((MainActivity)getActivity()).socketService_.sendMessage(jsonStr);
                                            }
                                        }
                                    });


                                }
                            }
                        }).setNegativeButton("취소",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 'No'
                                return;
                            }
                        });
                AlertDialog alert2 = alert_confirm.create();
                alert2.show();
                break;

            case R.id.imageView_profile_change: //프로필 변경 버튼
                imagePickerDialog_ = new ImagePickerDialog(getContext());
                imagePickerDialog_.show();
                break;
        }
    }

    @Override
    public void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        DebugHandler.log(getClass().getName(), "requestCode: " + requestCode);
        if(resultCode== Activity.RESULT_OK)
        {
            String imagePath = ""; // path 경로
            if(requestCode == REQ_CODE_SELECT_IMAGE) { //갤러리에서 액션 받은 경우
                Uri galleryImgUri = data.getData();
                imagePath = getRealPathFromURI(galleryImgUri); // path 경로
            }else if(requestCode == REQ_CODE_SELECT_CANVAS || requestCode == REQ_CODE_SELECT_CAMERA) { //캔버스 또는 카메라 에서 액션 받은 경우
                imagePath = data.getStringExtra("filePath");
            }else if(requestCode == REQ_CODE_PROFILE_FILTER) { //이미지 필터에서 액션 받은 경우
                imagePath = data.getStringExtra("filePath");
                uploadProfile(imagePath);
                return;
            }
            if(imagePath.equals("")){ //그냥 종료
                return;
            }
            //사진 촬영 / 앨범 / 그리기 다이어로그 종료
            imagePickerDialog_.dismiss();

            //필터 적용 액티비티
            Intent intent = new Intent(getContext(), ImageFilterActivity.class);
            intent.putExtra("imagePath", imagePath);
            getActivity().startActivityForResult(intent, REQ_CODE_PROFILE_FILTER);
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = ((MainActivity)getContext()).managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_account, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout: //로그아웃 버튼 클릭
                AlertDialog.Builder alert_confirm = new AlertDialog.Builder(getContext());
                alert_confirm.setMessage("로그아웃 하시겠습니까?").setCancelable(false).setPositiveButton("확인",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //소켓 연결 해제 요청
                                String[] keys = {""};
                                Object[] values = {""};
                                String jsonStr = JsonEncode.getInstance().encodeCommandJson("exitServer", keys, values);
                                if(((MainActivity)getActivity()).isLiveBinder) {
                                    ((MainActivity) getActivity()).socketService_.sendMessage(jsonStr);
                                }

                                //메인 화면으로 이동
                                ((MainActivity)getContext()).client_.logoutClient();
                                Intent intent = new Intent(getContext(), LoginActivity.class);
                                getActivity().startActivity(intent);
                                getActivity().finish();
                            }
                        }).setNegativeButton("취소",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 'No'
                                return;
                            }
                        });
                AlertDialog alert = alert_confirm.create();
                alert.show();
                break;

            case R.id.action_AR: //AR클릭
                Intent intent = new Intent(getContext(), MapsActivity.class);
                getActivity().startActivity(intent);
                getActivity().finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void uploadProfile(final String imagePath){
        DebugHandler.log(getClass().getName(), "RealPath: " + imagePath);
        long now = System.currentTimeMillis();
        final String fileName = account_.id_ + "_" + now + ".png";
        HashMap<String, String> postData = new HashMap<>();
        postData.put("filePath", "/usr/share/nginx/html/profile_image/");
        PostDB postDB = new PostDB(getContext());
        postDB.putFileData("upload_file.php", postData, imagePath, fileName, new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
                DebugHandler.log("ProfileUploadResult", output);
                if(output.equals("success")){ //파일 업로드 성공했으면 db에 저장
                    makeAndUploadThumbnail(imagePath, fileName); //썸네일 생성

                    //캐시 파일 삭제
                    if(new File(imagePath).exists()) {
                        DebugHandler.log(getClass().getName(), "DeleteFile: " + imagePath);
                        new File(imagePath).delete();
                    }
                }
            }
        });
    }

    private void makeAndUploadThumbnail(String imagePath, final String uploadFileName){
        Mat img_input = new Mat();
        //이미지 불러오기
        loadImage(imagePath, img_input.getNativeObjAddr(), 200);
        final String extStorageDirectory = getContext().getCacheDir().getAbsolutePath(); //저장 경로
        String saveFileName = "thumbnail.png"; //저장 할 이름 (저장 경로 포함)
        saveFileName.trim();
        saveImage(extStorageDirectory, saveFileName, img_input.getNativeObjAddr()); //이미지 캐시에 저장
        final String uploadFilePath = extStorageDirectory + "/" + saveFileName;

//        final String uploadFilePath = cacheFile.getAbsolutePath();
        HashMap<String, String> postData = new HashMap<>();
        postData.put("filePath", "/usr/share/nginx/html/profile_image/");
        PostDB postDB = new PostDB(getContext());
        //썸네일 이미지 서버로 전송
        postDB.putFileData("upload_file.php", postData, uploadFilePath, uploadFileName + "Thumb.png", new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
                DebugHandler.log("ThumbnailUploadResult", output);
                if(output.equals("success")){ //썸네일 파일 업로드 성공

                    HashMap<String, String> postData = new HashMap<String, String>();
                    postData.put("id", account_.id_);
                    postData.put("name", uploadFileName);

                    PostDB postDB = new PostDB();
                    postDB.putFileData("save_profile.php", postData, null, null, new OnFinishDBListener() {
                        @Override
                        public void onSuccess(String output) {
                            account_.profileUrl_ = String.format(PROFILE_PATH, uploadFileName);
                            String[] columnNames = {"profile"};
                            Object[] columnValues = {account_.profileUrl_};
                            String[] whereNames = {"id"};
                            Object[] whereValues = {account_.id_};
                            SQLiteHandler handler = new SQLiteHandler(getContext(), "project_one", null, 1);
                            handler.update("account_info", columnNames, columnValues, whereNames, whereValues, "and");
                            Glide.with(getContext())
                                    .load(account_.profileUrl_ + "Thumb.png")
                                    .error(R.mipmap.ic_launcher)
                                    .centerCrop()
                                    .crossFade()
                                    .bitmapTransform(new CropCircleTransformation(new CustomBitmapPool()))
                                    .into(imageView_profile);

                            Glide.with(getContext())
                                    .load(account_.profileUrl_ + "Thumb.png")
                                    .error(R.mipmap.ic_launcher)
                                    .bitmapTransform(new BlurTransformation(getContext()))
                                    .into(imageView_back);
                        }
                    });

                    if(new File(uploadFilePath).exists()) { //캐시에서 썸네일 삭제
                        DebugHandler.log(getClass().getName(), "DeleteFile: " + uploadFilePath);
                        new File(uploadFilePath).delete();
                    }
                }
            }
        });
    }
}