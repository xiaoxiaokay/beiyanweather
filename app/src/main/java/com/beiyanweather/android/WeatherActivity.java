package com.beiyanweather.android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.beiyanweather.android.gson.Forecast;
import com.beiyanweather.android.gson.Weather;
import com.beiyanweather.android.service.AutoUpdateService;
import com.beiyanweather.android.util.HttpUtil;
import com.beiyanweather.android.util.Utility;
import com.bumptech.glide.Glide;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weatherLayout;

    private TextView titleCity;

    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    private ImageView bingPicImg;

    public SwipeRefreshLayout swipeRefresh;

    private String mWeatherId;

    public DrawerLayout drawerLayout;

    private Button navButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 透明状态栏
        if(Build.VERSION.SDK_INT > 21) {
            /*
             * 调用getWindow().getDecorView()方法拿到当前活动的DecorView，
             * 调用它的setSystemUiVisibility()方法来改变系统UI的显示
             */
            getWindow().getDecorView().setSystemUiVisibility(
                     View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN    // 上面两个表示活动的布局会显示在状态栏上面
                    |View.SYSTEM_UI_LAYOUT_FLAGS
                    |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            // 调用setStatusBarColor()方法，将状态栏设置成透明色
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);

        // 初始化各控件
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);  // 获取ImageView的实例
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);  // 获取SwipeRefreshLayout的实例
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);   // 调用setColorSchemeResources()方法来设置一个下拉刷新进度条的颜色
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_button);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null){
            // 有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;   // 用于记录城市的天气id
            showWeatherInfo(weather);
        }else {
            // 无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            // 请求数据前暂时将ScrollView隐藏，避免空数据的界面看上去很奇怪
            weatherLayout.setVisibility(View.INVISIBLE);
            // 调用requestWeather()方法从服务器请求天气数据
            requestWeather(mWeatherId);
        }
        // 调用setOnRefreshListener()方法来设置一个下拉刷新的监听器
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            // 当触发了下拉刷新操作的时候，就会回调这个监听器的onRefresh()方法
            public void onRefresh() {
                // 调用requestWeather()方法请求天气信息
                requestWeather(mWeatherId);
            }
        });

        // 尝试从SharedPreferences中读取缓存的背景图片
        String bingPic = prefs.getString("bing_pic", null);
        // 如果有缓存，直接使用Glide来加载这张图片
        if (bingPic != null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        // 如果没有缓存，调用loadBingPic()方法去请求今日的必应背景图
        }else {
            loadBingPic();
        }

        // 在Button的点击事件中调用DrawerLayout的openDrawer()方法来打开滑动菜单
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

    }

    /**
     * 根据天气id请求城市天气信息
     */
    public void requestWeather(String weatherId) {

        // 使用参数中传入的天气id拼装出一个接口地址
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";

        // 调用HttpUtil.sendOkHttpRequest()方法来向改地址发出请求，服务器会将相应城市的天气信息以JSON格式返回
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                // 在onResponse()回调中先调用Utility.handleWeatherResponse()方法将返回的JSON数据转换成Weather对象
                final Weather weather = Utility.handleWeatherResponse(responseText);
                // 将当前线程切换到主线程
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    // 判断，如果服务器返回的status状态是ok，则说明请求天气成功，然后将返回的数据缓存到SharedPreferences中
                    if (weather != null && "ok".equals(weather.status)){
                        SharedPreferences.Editor editor = PreferenceManager
                                .getDefaultSharedPreferences(WeatherActivity.this)
                                .edit();
                        editor.putString("weather", responseText);
                        editor.apply();
                        mWeatherId = weather.basic.weatherId;
                        // 调用showWeatherInfo()方法来进行内容显示
                        showWeatherInfo(weather);
                    }else {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败",Toast.LENGTH_SHORT).show();
                    }
                    // 调用SwipeRefreshLayout的setRefreshing()方法并传入false，用于表示刷新事件结束，并隐藏刷新进度条
                    swipeRefresh.setRefreshing(false);
                    }
                });
            }
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败",Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
        // 每次请求天气信息的时候，同时刷新背景图片
        loadBingPic();
    }

    /**
     * 加载必应每日一图
     */
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        // 调用HttpUtil.sendOkHttpRequest()方法获取到必应背景图的链接
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String bingPic = response.body().string();
                // 将这个链接缓存到SharedPreferences中
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(WeatherActivity.this)
                        .edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                // 将当前线程切换到主线程
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 使用Gilde来加载这张图片
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 处理并展示Weather实体类中的数据
     */
    private void showWeatherInfo(Weather weather){

        // 从Weather对象中获取数据，并显示到相应的控件上
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();

        // 未来几天天气预报的部分使用了for循环来处理每天的天气信息
        for (Forecast forecast : weather.forecastList){
            // 在循环中动态加载forecast_item.xml布局并设置相应的数据，然后添加到父布局当中
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        //设置完所有数据后，将ScrollView重新变为可见
        weatherLayout.setVisibility(View.VISIBLE);

        /*
         * 加入启动AutoUpdateService这个服务的代码，只要选中了某个城市并成功更新天气信息之后，
         * AutoUpdateService就会一直在后台运行，并保证每8小时更新一次天气
         */
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }
}
