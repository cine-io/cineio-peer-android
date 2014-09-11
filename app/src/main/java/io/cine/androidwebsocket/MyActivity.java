package io.cine.androidwebsocket;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnectionFactory;

import java.util.Random;


public class MyActivity extends Activity {
    private static final String TAG = "AndroidWebsocketTest";

    private Handler mHandler;
    Runnable myTask = new Runnable() {
        @Override
        public void run() {
            String ping = "primus::ping::" + System.currentTimeMillis();
            Log.v(TAG, "SENDING PING - " + ping);
            Log.v(TAG, mWebSocket.isOpen() ? "socket open" : "socket closed");
            sendToWebsocket(ping);
            mHandler.postDelayed(this, 10000);
        }
    };
    private WebSocket mWebSocket;
    private StartRTC mStartRTC;

    private void sendToWebsocket(String data){
        data = "[\"" + data + "\"]";
        Log.v(TAG, data);
        mWebSocket.send(data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(myTask);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        mHandler = new Handler();
        PeerConnectionFactory.initializeAndroidGlobals(this, true, false);
        mStartRTC = new StartRTC();
        go();
    }

    private char randomCharacterFromDictionary(){
        String dictionary = "abcdefghijklmnopqrstuvwxyz0123456789_";
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
    private String getSignalingUrl(){
        Random r = new Random();
        int server = r.nextInt(1000);
        String connId = randomStringOfLength(8);
        return "http://192.168.1.114:8888/primus/"+server+"/"+connId+"/websocket";
    }

    private void go() {
        String url = getSignalingUrl();
        Log.v(TAG, url);
        String protocol = "ws";
        Log.v(TAG, "making request");
        AsyncHttpClient.getDefaultInstance().websocket(url, protocol, new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                Log.v(TAG, "completed");

                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }
                mWebSocket = webSocket;
                mStartRTC.setSignalingConnection(webSocket);
                mHandler.postDelayed(myTask, 1000);

                webSocket.setDataCallback(new DataCallback() {
                    @Override
                    public void onDataAvailable(DataEmitter dataEmitter, ByteBufferList byteBufferList) {
                        Log.v(TAG, "I got some bytes!");
                        // note that this data has been read
                        byteBufferList.recycle();
                    }
                });
                webSocket.setClosedCallback(new CompletedCallback(){

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
                                handleOpen();
                                return;
                            }
                            else if (s.startsWith("a")){
                                Log.v(TAG, "received array");
                                s = s.substring(1);
                                JSONArray r = new JSONArray(s);
                                Log.v(TAG, "parsed array");
                                response = new JSONObject(r.getString(0));
                            }else{
                                Log.v(TAG, "received something else");
                                return;
                            }
                            Log.v(TAG, "parsed response");
                            String action = response.getString("action");
                            Log.v(TAG, "action is: "+action);
                            if (action.equals("allservers")){
                                Log.v(TAG, "GOT ALL SERVERS");
                                gotAllServers(response);
                            } else {
                                Log.v(TAG, "Unknown action");
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

    private void handleOpen() {
        joinRoom("hello");
    }

    private void joinRoom(String room) {
        try {
            JSONObject j = new JSONObject();
            j.put("action", "join");
            j.put("room", room);
            mWebSocket.send(j.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void gotAllServers(JSONObject response) {
        try {
            JSONArray allServers = response.getJSONArray("data");
            for (int i = 0; i < allServers.length(); i++){
                JSONObject iceServerData = (JSONObject) allServers.get(i);
                String url = (String) iceServerData.get("url");
                Log.v(TAG, url);
                if (url.startsWith("stun:")){
                    mStartRTC.addIceServer(url);
                }else{
                    Log.v(TAG, "did not add ice server");
                }
            }
            mStartRTC.start();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
