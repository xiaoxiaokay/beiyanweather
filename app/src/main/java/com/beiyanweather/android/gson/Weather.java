package com.beiyanweather.android.gson;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Weather {

    // 对5个类进行引用
    public String status;

    public Basic basic;

    public AQI aqi;

    public Now now;

    public Suggestion suggestion;

    // daily_forecast包含的是一个数组，因此这里使用了List集合来引用 Forecast类
    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;

}
