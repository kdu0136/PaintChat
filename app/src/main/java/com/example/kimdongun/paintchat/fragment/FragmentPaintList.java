package com.example.kimdongun.paintchat.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.kimdongun.paintchat.R;

/**
 * Created by KimDongun on 2016-12-30.
 */

public class FragmentPaintList extends Fragment {
    private ImageView imageView;
    public String fileName_; //파일 명

    public FragmentPaintList() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_paint_list, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imageView = (ImageView) view.findViewById(R.id.imageView);
        Glide.with(this)
                .load(fileName_)
                .placeholder(R.drawable.prepare_image)
                .error(R.drawable.prepare_image)
                .into(imageView);
    }
}