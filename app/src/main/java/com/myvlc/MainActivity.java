package com.myvlc;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends VLCBasePlayerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((LinearLayout)findViewById(R.id.videoFather)).addView(addVideoView(0));
    }

    @Override
    public RelativeLayout setFullViewContainer() {
        return (RelativeLayout)findViewById(R.id.videoFullSizeFather);
    }

    @Override
    public Map<Integer, String> setVideoPaths() {
        Map<Integer, String> paths = new HashMap<>();
        paths.put(0,"http://devimages.apple.com/iphone/samples/bipbop/bipbopall.m3u8");
        return paths;
    }

    @Override
    public boolean onKeyDownE(int keyCode, KeyEvent event) {
        return false;
    }
}
