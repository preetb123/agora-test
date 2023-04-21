package com.example.test;

import static io.agora.rtm.RtmStatusCode.ConnectionState.CONNECTION_STATE_DISCONNECTED;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.agora.rtm.RtmChannelAttribute;
import io.agora.rtm.RtmChannelListener;
import io.agora.rtm.RtmChannelMember;
import io.agora.rtm.RtmFileMessage;
import io.agora.rtm.RtmImageMessage;
import io.agora.rtm.RtmMessage;
import io.agora.rtm.RtmStatusCode;
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

    public static void gen(Context context, String channelName,  int uid, OnTokenGenCallback callback){
        gen(context.getString(R.string.agora_app_id), context.getString(R.string.agora_app_certificate), channelName, uid, callback);
    }

    private static void gen(String fd, String fdf, int i, Object o, Object o1) {
    }

    private static void runOnUiThread(@NonNull Runnable runnable){
        if(Thread.currentThread() == Looper.getMainLooper().getThread()){
            runnable.run();
        }else{
            new Handler(Looper.getMainLooper()).post(runnable);
        }
    }

    private static void gen(String appId, String certificate, String channelName, int uid, OnTokenGenCallback callback)  {
        if(TextUtils.isEmpty(appId) || TextUtils.isEmpty(certificate) || TextUtils.isEmpty(channelName)){
            if(callback != null){
                callback.onError(new IllegalArgumentException("appId=" + appId + ", certificate=" + certificate + ", channelName=" + channelName));
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
            if(callback != null){
                callback.onError(e);
            }
        }

        // http://localhost:8080/rtc/testChannel/publisher/uid/1

        Request request = new Request.Builder()
                .url("https://panicky-bone-production.up.railway.app/rte/" + channelName + "/publisher/uid/"+uid + "?expiry=3600")
                .addHeader("Content-Type", "application/json")
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if(callback != null){
                    callback.onError(e);
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(body.string());
                        String data = jsonObject.getString("rtcToken");
                        String rtmToken = null;
                        if(jsonObject.has("rtmToken")){
                            rtmToken = jsonObject.getString("rtmToken");
                        }
                        if(callback != null){
                            callback.onTokenGen(data, rtmToken);
                        }
                    } catch (Exception e) {
                        if(callback != null){
                            callback.onError(e);
                        }
                    }
                }
            }
        });

        Map<Integer, String> map = new HashMap<Integer, String>(){{
            put(1, "CONNECTION_STATE_DISCONNECTED");
            put(2, "CONNECTION_STATE_CONNECTING");
            put(3, "CONNECTION_STATE_CONNECTED");
            put(4, "CONNECTION_STATE_RECONNECTING");
            put(5, "CONNECTION_STATE_ABORTED");
        }};


    }



    public interface OnTokenGenCallback {
        void onTokenGen(String rtcToken, String rtmToken);
        void onError(Exception error);
    }
}
