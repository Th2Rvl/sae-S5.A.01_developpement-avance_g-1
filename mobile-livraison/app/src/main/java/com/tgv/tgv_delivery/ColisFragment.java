package com.tgv.tgv_delivery;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

/**
 */
public class ColisFragment extends Fragment implements View.OnClickListener  {


    public static ColisFragment newInstance() {
        ColisFragment fragment = new ColisFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View vueDuFragment = inflater.inflate(R.layout.fragment_colis, container, false);

        return vueDuFragment;
    }

    @Override
    public void onClick(View v){

    }
}