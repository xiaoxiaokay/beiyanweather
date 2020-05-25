package com.beiyanweather.android;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * @author: 潇潇
 * @project: 贝言天气
 * @Package com.beiyanweather.android
 * @Description: 天气预报
 * @date: 2020/5/23
 * @version: V1.0
 */

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
         * 从SharedPreferences文件中读取缓存数据，如果不为null就说明之前已经请求过天气数据了，
         * 就没必要让用户再次选择城市，而是直接跳转到WeatherActivity即可
         */
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getString("weather", null) != null){
            Intent intent = new Intent(this, WeatherActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
