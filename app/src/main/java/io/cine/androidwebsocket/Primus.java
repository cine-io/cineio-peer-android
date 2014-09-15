package io.cine.androidwebsocket;

import android.os.Handler;
import android.util.Log;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

/**
 * Created by thomas on 9/15/14.
 */
public class Primus {
    private static final String TAG = "Primus";
    private Handler mHandler;
    public WebSocket webSocket;

    private final String baseUrl;
    private final static String dictionary = "abcdefghijklmnopqrstuvwxyz0123456789_";
    private final String url;
    private PrimusDataCallback dataCallback;
    private PrimusOpenCallback openCallback;
    private PrimusWebSocketCallback websocketCallback;

    private Primus(String baseUrl) {
        this.baseUrl = baseUrl;
        url = generateSignalingUrl();
        Log.v(TAG, url);
        mHandler = new Handler();

        connect();
    }

    public void setDataCallback(Primus.PrimusDataCallback callback){
        this.dataCallback = callback;
    }

    public void setWebSocketCallback(Primus.PrimusWebSocketCallback callback){
        this.websocketCallback = callback;
    }

    public void setOpenCallback(PrimusOpenCallback callback){
        this.openCallback = callback;
    }

    private void connect(){
        String protocol = "ws";
        AsyncHttpClient.getDefaultInstance().websocket(url, protocol, new AsyncHttpClient.WebSocketConnectCallback() {

            @Override
            public void onCompleted(Exception ex, WebSocket returnedWebsocket) {
                Log.v(TAG, "completed");

                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }
                webSocket = returnedWebsocket;
                mHandler.postDelayed(myTask, 1000);
                if (websocketCallback != null){
                    websocketCallback.onWebSocket(webSocket);
                }

                webSocket.setDataCallback(new DataCallback() {
                    @Override
                    public void onDataAvailable(DataEmitter dataEmitter, ByteBufferList byteBufferList) {
                        Log.v(TAG, "I got some bytes!");
                        // note that this data has been read
                        byteBufferList.recycle();
                    }
                });

                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception e) {
                        if (e != null) {
                            e.printStackTrace();
                            return;
                        }
                        Log.v(TAG, "ws: closedCallback onCompleted");
                    }
                });
                webSocket.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception e) {
                        if (e != null) {
                            e.printStackTrace();
                            return;
                        }

                        Log.d(TAG, "ws: endCallback onCompleted");
                    }
                });
                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    public void onStringAvailable(String s) {
                        Log.v(TAG, "got string: " + s);
                        try {
                            JSONObject response;
                            if (s.startsWith("o")){
                                if (openCallback != null){
                                    openCallback.onOpen();
                                }
                            }
                            else if (s.startsWith("a")){
                                Log.v(TAG, "received array");
                                s = s.substring(1);
                                JSONArray r = new JSONArray(s);
                                Log.v(TAG, "parsed array");
                                response = new JSONObject(r.getString(0));
                                if (dataCallback != null){
                                    dataCallback.onData(response);
                                }
                            }else{
                                Log.v(TAG, "received something else");
                                return;
                            }
                        } catch (JSONException e) {
                            Log.v(TAG, "UNABLE TO PARSE RESPONSE");
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }


    public void joinRoom(String room) {
        try {
            JSONObject j = new JSONObject();
            j.put("action", "join");
            j.put("room", room);
            webSocket.send(j.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static Primus connect(String url){
        return new Primus(url);
    }

    private char randomCharacterFromDictionary(){
        int rand = (int)(Math.random() * dictionary.length());
        return dictionary.charAt(rand);
    }

    private String randomStringOfLength(int length){
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < length; i++){
            s.append(randomCharacterFromDictionary());
        }
        return s.toString();
    }

    // "ws://cine-io-signaling.herokuapp.com/primus/211/b9__ftym/websocket"
    private String generateSignalingUrl(){
        Random r = new Random();
        int server = r.nextInt(1000);
        String connId = randomStringOfLength(8);
        return baseUrl+"/"+server+"/"+connId+"/websocket";
    }
    Runnable myTask = new Runnable() {
        @Override
        public void run() {
            String ping = "primus::ping::" + System.currentTimeMillis();
//            Log.v(TAG, "SENDING PING - " + ping);
            Log.v(TAG, webSocket.isOpen() ? "socket open" : "socket closed");
            sendToWebsocket(ping);
            mHandler.postDelayed(this, 10000);
        }
    };

    private void sendToWebsocket(String data){
        data = "[\"" + data + "\"]";
        Log.v(TAG, data);
        webSocket.send(data);
    }

    public void onPause() {
        mHandler.removeCallbacks(myTask);
    }

    public static interface PrimusWebSocketCallback {
        public void onWebSocket(WebSocket websocket);
    }
    public static interface PrimusOpenCallback {
        public void onOpen();
    }
    public static interface PrimusDataCallback {
        public void onData(JSONObject jsonObject);
    }
}
