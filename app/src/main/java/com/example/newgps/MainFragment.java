package com.example.newgps;

//AndroidX
//import androidx.fragment.app.Fragment;
//import androidx.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.annotation.NonNull;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainFragment extends Fragment {



    public static MainFragment newInstance(String str){
        Log.d("testfragment", "testfragment");
        // Fragemnt01 インスタンス生成
        MainFragment fragment = new MainFragment();
        // Bundle にパラメータを設定
        Bundle barg = new Bundle();
        barg.putString("Message", str);
        fragment.setArguments(barg);

        return fragment;
    }

    // FragmentのViewを生成して返す
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container2,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_main,
                container2, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if(args != null ){
            String str = args.getString("Message");
            TextView textView = view.findViewById(R.id.text_fragment);
            textView.setText(str);
        }
    }
}