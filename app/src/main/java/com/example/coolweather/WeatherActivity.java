package com.example.coolweather;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.bumptech.glide.*;
import com.example.coolweather.gson.Forecast;
import com.example.coolweather.gson.Weather;
import com.example.coolweather.service.AutoUpdateService;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    public DrawerLayout drawerLayout;

    public Button navButton;

//    private ImageView bingPicImg;

    public SwipeRefreshLayout swipeRefresh;

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

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // 初始化各个控件
//        bingPicImg=(ImageView) findViewById(R.id.bing_pic_image);
        swipeRefresh=(SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeColors(R.color.colorPrimary);
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
        navButton=(Button) findViewById(R.id.nav_button);
        drawerLayout=(DrawerLayout) findViewById(R.id.drawer_layout);

        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString=prefs.getString("weather",null);
        String bingPic=prefs.getString("bing_pic",null);
//        if(bingPic!=null){
//            Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
//        } else {
//            loadBingPic();
//        }
        final String weatherId;
        if(weatherString!=null){
            // 有缓存时直接解析天气数据
            Weather weather= Utility.handleWeatherResponse(weatherString);
            weatherId=weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            // 无缓存时去服务器查询天气
           weatherId=getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });
    }
    // 根据weatherId向服务器请求城市天气信息
    public void requestWeather(final String weatherId){
        String weatherUrl="http://guolin.tech/api/weather?cityid="+weatherId+
                "&key=d2025e4b54c6414ab82e6fb8ec9ddfd8";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText=response.body().string();
                final Weather weather=Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null&&"ok".equals(weather.status)){
                            SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
//        loadBingPic();
    }
    // 处理并知识Weather实体类中的数据
    private void showWeatherInfo(Weather weather){

        if(weather!=null&&"ok".equals(weather.status)){
            String cityName = weather.basic.cityName;
            String updateTime = weather.basic.update.updateTime.split("")[1];
            String degree = weather.now.temperature +"℃";
            String weatherInfo = weather.now.more.info;
            titleCity.setText(cityName);
            titleUpdateTime.setText(updateTime);
            degreeText.setText(degree);
            weatherInfoText.setText(weatherInfo);
            forecastLayout.removeAllViews();
            for(Forecast forecast:weather.forecastList){
                View view= LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
                TextView dateText = (TextView)view.findViewById(R.id.date_text);
                TextView infoText = (TextView)view.findViewById(R.id.info_text);
                TextView maxText = (TextView)view.findViewById(R.id.max_text);
                TextView minText = (TextView)view.findViewById(R.id.min_text);
                dateText.setText(forecast.date);
                infoText.setText(forecast.more.info);
                maxText.setText(forecast.temperature.max);
                minText.setText(forecast.temperature.min);
                forecastLayout.addView(view);
            }
            if(weather.aqi!=null){
                aqiText.setText(weather.aqi.city.aqi);
                pm25Text.setText(weather.aqi.city.aqi);
            }
//        String comfort="舒适度:"+weather.suggestion.comfort.info;
            String carWash ="洗车指数："+weather.suggestion.carWash.info;
            String sport ="运动建议："+weather.suggestion.sport.info;
//        comfortText.setText(comfort);
            carWashText.setText(carWash);
            sportText.setText(sport);
            weatherLayout.setVisibility(View.VISIBLE);

            // 每次刷新启动服务
            Intent intent=new Intent(this, AutoUpdateService.class);
            startService(intent);
        }else {
            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
        }
    }

    // 加载每日bing一图
//    private void loadBingPic(){
//        String requestBingPic="http://guolin.tech/api/bing_pic";
//        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                final String bingPic=response.body().string();
//                SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
//                editor.putString("bing_pic",bingPic);
//                editor.apply();
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
//                    }
//                });
//            }
//
//            @Override
//            public void onFailure(Call call, IOException e) {
//                e.printStackTrace();
//            }
//        });
//    }
}