package com.example.kimdongun.paintchat.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.activity.ChatRoomActivity;
import com.example.kimdongun.paintchat.activity.MainActivity;
import com.example.kimdongun.paintchat.adapter.ChatRoomListViewAdapter;
import com.example.kimdongun.paintchat.item.ChatRoomListViewItem;

/**
 * Created by KimDongun on 2016-12-30.
 */

public class FragmentChat extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener{
    private ListView myListView_; //채팅 방 리스트
    private ChatRoomListViewAdapter adapter_; //채팅 방 리스트 뷰 어댑터
    private Client client_; //클라이언트 정보

    public FragmentChat() {
        // Required empty public constructor
    }
    @Override
    public String toString() {
        return "FragmentChat";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        //초기화
        client_ = ((MainActivity)getContext()).client_;
        myListView_ = (ListView)view.findViewById(R.id.listView);
        adapter_ = client_.chatRoomListViewAdapter;

        myListView_.setAdapter(adapter_);
        myListView_.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
        ChatRoomListViewItem item = (ChatRoomListViewItem)adapter_.getItem(index);

        Intent intent = new Intent(getActivity(), ChatRoomActivity.class);
        intent.putExtra("roomName", item.accountNick_); //채팅방 이름
        intent.putExtra("key", item.roomKey_); //채팅방 키값 전송
        startActivity(intent);
        getActivity().finish();
    }
}