package com.example.kimdongun.paintchat.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.CustomBitmapPool;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.activity.MainActivity;
import com.example.kimdongun.paintchat.activity.ProfileActivity;
import com.example.kimdongun.paintchat.activity.RecommendSocialActivity;
import com.example.kimdongun.paintchat.adapter.SocialListViewAdapter;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by KimDongun on 2016-12-30.
 */

public class FragmentSocial extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener{
    public ListView listView_social; //친구 리스트
    private SocialListViewAdapter socialListViewAdapter; //친구 리스트 뷰 어댑터
    private SocialListViewAdapter recommendSocialListViewAdapter; //추천 친구 리스트 뷰 어댑터
    private LinearLayout layout_social_list; //친구 레이아웃

    private View header_; //리스트뷰 헤더(추천 친구 공간)
    private LinearLayout layout_profile; //프로필 레이아웃
    private ImageView imageView_profile; //내 프로필 이미지
    private TextView textView_nick; //내 프로필 닉네임
    private LinearLayout layout_recommend; //추천 친구 레이아웃
    private ImageView imageView_recommend; //추천 친구 이미지
    private TextView textView_recommend_num; //추천 친구 수

    private LinearLayout layout_socialText; //친구 텍스트 레이아웃
    private LinearLayout layout_recommendText; //추천 친구 텍스트 레이아웃
    private LinearLayout layout_profileText; //프로필 텍스트 레이아웃

    private Client client_; //클라이언트 정보

    public FragmentSocial() {
        // Required empty public constructor
    }

    @Override
    public String toString() {
        return "FragmentSocial";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_social, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        //초기화
        header_ = getLayoutInflater(savedInstanceState).inflate(R.layout.social_listview_header, null, false);

        client_ = ((MainActivity)getContext()).client_;
        socialListViewAdapter = client_.socialListViewAdapter;
        recommendSocialListViewAdapter = client_.recommendSocialListViewAdapter;
        listView_social = (ListView) view.findViewById(R.id.listView_social);
        layout_social_list = (LinearLayout)view.findViewById(R.id.layout_social_list);

        //header에 있는 뷰
        layout_profile = (LinearLayout)header_.findViewById(R.id.layout_profile);
        imageView_profile = (ImageView)header_.findViewById(R.id.imageView_profile);
        textView_nick = (TextView) header_.findViewById(R.id.textView_nick);
        layout_recommend = (LinearLayout)header_.findViewById(R.id.layout_recommend);
        imageView_recommend = (ImageView)header_.findViewById(R.id.imageView_recommend);
        textView_recommend_num = (TextView) header_.findViewById(R.id.textView_recommend_num);

        layout_socialText = (LinearLayout)header_.findViewById(R.id.layout_socialText);
        layout_recommendText = (LinearLayout)header_.findViewById(R.id.layout_recommendText);
        layout_profileText = (LinearLayout)header_.findViewById(R.id.layout_profileText);

        //리스트뷰 헤더 설정
        listView_social.addHeaderView(header_);

        //프로필 값 대입
        Glide.with(this).load(client_.account_.profileUrl_ + "Thumb.png")
                .thumbnail(0.1f)
                .centerCrop()
                .placeholder(R.drawable.prepare_image)
                .error(R.mipmap.ic_launcher)
                .bitmapTransform(new CropCircleTransformation(new CustomBitmapPool()))
                .into(imageView_profile);
        textView_nick.setText(client_.account_.nick_);

        //추천 친구 이미지
        Glide.with(this).load(client_.account_.profileUrl_ + "Thumb.png")
                .thumbnail(0.1f)
                .centerCrop()
                .placeholder(R.drawable.prepare_image)
                .error(R.mipmap.ic_launcher)
                .bitmapTransform(new CropCircleTransformation(new CustomBitmapPool()))
                .into(imageView_recommend);

        //추천 친구 수 대입
        textView_recommend_num.setText(String.valueOf(recommendSocialListViewAdapter.getCount()));
        if(recommendSocialListViewAdapter.getCount() == 0){ //추천 친구가 없으면 초기에 추천 친구 창 안보이게
            layout_recommend.setVisibility(View.GONE);
        }

        //어댑터 설정
        listView_social.setAdapter(socialListViewAdapter);

        //터치 이벤트
        layout_profileText.setOnClickListener(this);
        layout_recommendText.setOnClickListener(this);
        layout_socialText.setOnClickListener(this);
        layout_profile.setOnClickListener(this);
        layout_recommend.setOnClickListener(this);
        listView_social.setOnItemClickListener(this);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.layout_profileText: //프로필 텍스트 레이아웃 클릭 -> 프로필이 보이면 안보이게, 안보이면 보이게
                if(layout_profile.getVisibility() == View.GONE){
                    layout_profile.setVisibility(View.VISIBLE);
                }else{
                    layout_profile.setVisibility(View.GONE);
                }
                break;

            case R.id.layout_recommendText: //추천 친구 텍스트 레이아웃 클릭 -> 추천 친구가 보이면 안보이게, 안보이면 보이게
                if(layout_recommend.getVisibility() == View.GONE){
                    layout_recommend.setVisibility(View.VISIBLE);
                }else{
                    layout_recommend.setVisibility(View.GONE);
                }
                break;

            case R.id.layout_socialText: //친구 텍스트 레이아웃 클릭 -> 친구가 보이면 안보이게, 안보이면 보이게
                LinearLayout.LayoutParams social_params = (LinearLayout.LayoutParams)layout_social_list.getLayoutParams();
                if(social_params.height == header_.getMeasuredHeight()){
                    social_params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }else{
                    social_params.height = header_.getMeasuredHeight();
                }
                layout_social_list.setLayoutParams(social_params);
                break;

            case R.id.layout_profile: //프로필 레이아웃 클릭 -> 프로필 화면
                Intent intent = new Intent(getActivity(), ProfileActivity.class);
                intent.putExtra("accountId", client_.account_.id_);
                intent.putExtra("type", "mine");
                intent.putExtra("from", "main");
                startActivity(intent);
                getActivity().finish();
                break;

            case R.id.layout_recommend: //추천 친구 레이아웃 클릭 -> 추천 친구 화면으로
                if(client_.recommendSocialListViewAdapter.getCount() < 1){
                    Toast.makeText(getContext(), "추천 친구가 없습니다.", Toast.LENGTH_SHORT).show();;
                }else {
                    Intent intent2 = new Intent(getActivity(), RecommendSocialActivity.class);
                    startActivity(intent2);
                    getActivity().finish();
                }
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_social, menu);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
        Account account = (Account)listView_social.getItemAtPosition(index);
        if(account != null) {
            Intent intent = new Intent(getActivity(), ProfileActivity.class);
            intent.putExtra("accountId", account.id_);
            intent.putExtra("type", "social");
            intent.putExtra("from", "main");
            startActivity(intent);
            getActivity().finish();
        }
    }
}