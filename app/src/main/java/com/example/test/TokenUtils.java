package com.example.test;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

public class TokenUtils {
    private final String TAG = "TokenGenerator";
    private final static OkHttpClient client;

    static {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();
    }

    public static void gen(Context context, String channelName,  int uid, OnTokenGenCallback<String> onGetToken){
        gen(context.getString(R.string.agora_app_id), context.getString(R.string.agora_app_certificate), channelName, uid, ret -> {
            if(onGetToken != null){
                runOnUiThread(() -> {
                    onGetToken.onTokenGen(ret);
                });
            }
        }, ret -> {
            Log.e("TAG", "for requesting token error, use config token instead.");
            if (onGetToken != null) {
                runOnUiThread(() -> {
                    onGetToken.onTokenGen(null);
                });
            }
        });
    }

    private static void runOnUiThread(@NonNull Runnable runnable){
        if(Thread.currentThread() == Looper.getMainLooper().getThread()){
            runnable.run();
        }else{
            new Handler(Looper.getMainLooper()).post(runnable);
        }
    }

    private static void gen(String appId, String certificate, String channelName, int uid, OnTokenGenCallback<String> onGetToken, OnTokenGenCallback<Exception> onError)  {
        if(TextUtils.isEmpty(appId) || TextUtils.isEmpty(certificate) || TextUtils.isEmpty(channelName)){
            if(onError != null){
                onError.onTokenGen(new IllegalArgumentException("appId=" + appId + ", certificate=" + certificate + ", channelName=" + channelName));
            }
            return;
        }

        JSONObject postBody = new JSONObject();
        try {
            postBody.put("appId", appId);
            postBody.put("appCertificate", certificate);
            postBody.put("channelName", channelName);
            postBody.put("expire", 900);// s
            postBody.put("src", "Android");
            postBody.put("ts", System.currentTimeMillis() + "");
            postBody.put("type", 1); // 1: RTC Token ; 2: RTM Token
            postBody.put("uid", uid + "");
        } catch (JSONException e) {
            if(onError != null){
                onError.onTokenGen(e);
            }
        }

        // http://localhost:8080/rtc/testChannel/publisher/uid/1

        Request request = new Request.Builder()
                .url("https://panicky-bone-production.up.railway.app/rtc/" + channelName + "/publisher/uid/"+uid)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if(onError != null){
                    onError.onTokenGen(e);
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(body.string());
                        String data = jsonObject.getString("rtcToken");
                        if(onGetToken != null){
                            onGetToken.onTokenGen(data);
                        }
                    } catch (Exception e) {
                        if(onError != null){
                            onError.onTokenGen(e);
                        }
                    }
                }
            }
        });
    }

    public interface OnTokenGenCallback<T> {
        void onTokenGen(T ret);
    }
}
