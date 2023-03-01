package com.example.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.example.coolweather.gson.Weather;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateBingPic();
        updateWeather();
        AlarmManager manager=(AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour=8*60*60*1000;// 八小时更新一次
        long triggerAtTime= SystemClock.elapsedRealtime()+anHour;
        Intent i=new Intent(this,AutoUpdateService.class);
        PendingIntent pi;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pi = PendingIntent.getActivity(this, 123, i, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pi = PendingIntent.getActivity(this, 123, i, PendingIntent.FLAG_ONE_SHOT);
        }
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);

        return super.onStartCommand(intent, flags, startId);
    }
    // 更新天气信息
    private void updateWeather(){
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString=prefs.getString("weather",null);
        if(weatherString!=null){
            // 有缓存的时候直接解析天气数据
            Weather weather=Utility.handleWeatherResponse(weatherString);
            String weatherId=weather.basic.weatherId;
            String weatherUrl="http://quolin.tech/api/weather?citvid="+weatherId+"&key=d2025e4b54c6414ab82e6fb8ec9ddfd8";
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText=response.body().toString();
                    Weather weather=Utility.handleWeatherResponse(responseText);
                    if(weather!=null && "ok".equals(weather.status)){
                        SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather",responseText);
                        editor.apply();
                    }
                }
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
    // 更新背景图片（暂时不做）
    private void updateBingPic(){
    }
}