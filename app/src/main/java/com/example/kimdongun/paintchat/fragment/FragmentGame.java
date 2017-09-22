package com.example.kimdongun.paintchat.fragment;

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
import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.JsonEncode;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.activity.MainActivity;
import com.example.kimdongun.paintchat.adapter.GameRoomListViewAdapter;
import com.example.kimdongun.paintchat.dialog.EnterGameRoomDialog;
import com.example.kimdongun.paintchat.item.GameRoomListViewItem;

/**
 * Created by KimDongun on 2016-12-30.
 */

public class FragmentGame extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener{
    private ListView myListView_; //게임 방 리스트
    private GameRoomListViewAdapter adapter_; //게임 방 리스트 뷰 어댑터
    private Client client_; //클라이언트 정보

    private EnterGameRoomDialog enterGameRoomDialog_; //방 입장 다이얼로그

    public FragmentGame() {
        // Required empty public constructor
    }
    @Override
    public String toString() {
        return "FragmentGame";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_game, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        //초기화
        client_ = ((MainActivity)getContext()).client_;
        myListView_ = (ListView)view.findViewById(R.id.listView);
        adapter_ = client_.gameRoomListViewAdapter;

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
        inflater.inflate(R.menu.menu_game, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort: //정렬 버튼 클릭
                String[] keys = {""};
                Object[] values = {""};
                String jsonStr = JsonEncode.getInstance().encodeCommandJson("gameRoomList", keys, values);
                //서버로 전송
                if(((MainActivity)getActivity()).isLiveBinder)
                    ((MainActivity)getActivity()).socketService_.sendMessage(jsonStr);
               break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
        GameRoomListViewItem item = (GameRoomListViewItem)adapter_.getItem(index);

        if(item.isLock_){ //잠긴 방이면 비밀번호 입력하는 다이얼로그 출력
            enterGameRoomDialog_ = new EnterGameRoomDialog(getContext(), client_, item.key_, item.name_);
            enterGameRoomDialog_.show();
        }else {
            String[] keys = {"key", "password"};
            Object[] values = {item.key_, "password"};

            String jsonStr = JsonEncode.getInstance().encodeCommandJson("enterGameRoom", keys, values);
            DebugHandler.log(getClass().getName(), jsonStr);

            //서버로 전송
            if (((MainActivity) getActivity()).isLiveBinder)
                ((MainActivity) getActivity()).socketService_.sendMessage(jsonStr);
        }
    }
}