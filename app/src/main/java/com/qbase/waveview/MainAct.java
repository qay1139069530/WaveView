package com.qbase.waveview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainAct extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main);
        WaveView mWaveView =findViewById(R.id.waveView);
        mWaveView.startAnimation();
    }
}
