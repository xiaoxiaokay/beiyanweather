package com.beiyanweather.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.beiyanweather.android.db.City;
import com.beiyanweather.android.db.County;
import com.beiyanweather.android.db.Province;
import com.beiyanweather.android.gson.Weather;
import com.beiyanweather.android.util.HttpUtil;
import com.beiyanweather.android.util.Utility;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;

    private TextView titleText;

    private Button backButton;

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;

    /**
     * 县列表
     */
    private List<County> countyList;

    /**
     * 选中的省份
     */
    private Province selectedProvince;

    /**
     * 选中的城市
     */
    private City selectedCity;

    /**
     * 当前选中的级别
     */
    private int currentLevel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        // 获取控件实例
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        // 初始化ArrayAdapter，并将它设置为ListView的适配器
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // ListView点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            /*
             * 当点击了某个省的时候会进入到ListView的onItemClick()方法中，
             * 这时，会根据当前的级别来判断是去调用queryCities()方法还是queryCounties()方法，
             */
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE)
                {
                    // queryCities()方法是查询市级数据
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }
                else if (currentLevel == LEVEL_CITY)
                {
                    // queryCounties()方法是查询县级数据
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
                else if (currentLevel == LEVEL_COUNTY)
                {
                    String weatherId = countyList.get(position).getWeatherId();
                    if (getActivity() instanceof MainActivity)
                    {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if (getActivity() instanceof WeatherActivity)
                    {
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }
                }
            }
        });
        // Button点击事件
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY)
                {
                    queryCities();
                }
                else if (currentLevel == LEVEL_CITY)
                {
                    queryProvinces();
                }
            }
        });
        // 调用queryProvinces()方法，从这里开始加载省级数据
        queryProvinces();
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryProvinces() {
        // 将头布局的标题设置为中国
        titleText.setText("中国");
        // 因为省级列表不能再返回了，所以将返回按钮隐藏起来
        backButton.setVisibility(View.GONE);
        // 调用LitePal的查询接口来从数据库中读取省级数据
        provinceList = LitePal.findAll(Province.class);
        // 如果读到了，直接将数据显示到界面上
        if (provinceList.size() > 0)
        {
            dataList.clear();
            for (Province province : provinceList)
            {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        // 如果没有读取到数据，组装出一个请求地址
        }else {
            String address = "http://guolin.tech/api/china";
            // 调用queryFromServer()方法从服务器查询数据
            queryFromServer(address, "province");
        }
    }

    /**
     * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0)
        {
            dataList.clear();
            for (City city : cityList)
            {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }
        else
        {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    /**
     * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0)
        {
            dataList.clear();
            for (County county : countyList)
            {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }
        else
        {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    /**
     * 根据传入的地址和类型从服务器上查询省市县数据
     */
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        // 调用HttpUtil的sendOkHttpRequest()方法向服务器发送请求
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            // 响应的数据会回调到onResponse()方法中
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type))
                {
                    // 调用Utility的handleProvinceResponse()方法来解析和处理服务器返回的数据，并存储到数据库中
                    result = Utility.handleProvinceResponse(responseText);
                }else if ("city".equals(type)){
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                }else if ("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }
                if (result)
                {
                    /**
                     * 再次调用queryProvinces()方法重新加载省级数据。
                     * 由于queryProvinces()方法牵扯到了UI操作，所以必须要在主线程中调用，
                     * 这里借助了runOnUiThread()方法来实现从子线程切换到主线程。
                     */
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)){
                                queryProvinces();
                            }else if ("city".equals(type)){
                                queryCities();
                            }else  if ("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call call, IOException e) {
                // 通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null)
        {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null)
        {
            progressDialog.dismiss();
        }
    }
}
