package io.cine.androidwebsocket;

import android.app.Activity;
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
    private final static String dictionary = "abcdefghijklmnopqrstuvwxyz0123456789_";
    private final Activity activity;
    private final String baseUrl;
    private final String url;
    public WebSocket webSocket;
    private Handler mHandler;
    private PrimusDataCallback dataCallback;
    private PrimusOpenCallback openCallback;
    private PrimusWebSocketCallback websocketCallback;
    private int currentTimerRun;
    Runnable myTask = new Runnable() {
        @Override
        public void run() {
            currentTimerRun++;
            // send a heartbeat every 10 seconds
            if (!webSocket.isOpen()) {
                currentTimerRun = 0;
                Log.v(TAG, "Reconnecting to primus");
                throw new RuntimeException("SOCKET IS CLOSED! FUCKED!");
            }
            if (currentTimerRun >= 10) {
                currentTimerRun = 0;
                String ping = "primus::ping::" + System.currentTimeMillis();
                // Log.v(TAG, "SENDING PING - " + ping);
                Log.v(TAG, webSocket.isOpen() ? "socket open" : "socket closed");
                sendToWebsocket(ping);
            } else {
//                Log.v(TAG, "did not send ping: "+currentTimerRun);
            }
            scheduleHeartbeat();
        }
    };

    private Primus(Activity activity, String baseUrl) {
        this.activity = activity;
        this.baseUrl = baseUrl;
        url = generateSignalingUrl();
        Log.v(TAG, url);
        mHandler = new Handler();
        currentTimerRun = 0;
        reconnect();
    }

    public static Primus connect(MyActivity activity, String url) {
        return new Primus(activity, url);
    }

    public void setDataCallback(Primus.PrimusDataCallback callback) {
        this.dataCallback = callback;
    }

    public void setWebSocketCallback(Primus.PrimusWebSocketCallback callback) {
        this.websocketCallback = callback;
    }

    public void setOpenCallback(PrimusOpenCallback callback) {
        this.openCallback = callback;
    }

    // TODO: return ws on http and wss on https
    private String getProtocolFromUrl() {
        return "ws";
    }

    private void reconnect() {
        AsyncHttpClient.getDefaultInstance().websocket(url, getProtocolFromUrl(), new AsyncHttpClient.WebSocketConnectCallback() {

            @Override
            public void onCompleted(Exception ex, WebSocket returnedWebsocket) {
                Log.v(TAG, "completed");

                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }
                webSocket = returnedWebsocket;

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
                            if (s.startsWith("o")) {
                                if (openCallback != null) {
                                    openCallback.onOpen();
                                }
                            } else if (s.startsWith("a")) {
                                Log.v(TAG, "received array");
                                s = s.substring(1);
                                JSONArray r = new JSONArray(s);
                                Log.v(TAG, "parsed array");
                                response = new JSONObject(r.getString(0));
                                if (dataCallback != null) {
                                    dataCallback.onData(response);
                                }
                            } else {
                                Log.v(TAG, "received something else");
                                return;
                            }
                        } catch (JSONException e) {
                            Log.v(TAG, "UNABLE TO PARSE RESPONSE");
                            e.printStackTrace();
                        }
                    }
                });
                scheduleHeartbeat();
                if (websocketCallback != null) {
                    websocketCallback.onWebSocket(webSocket);
                }

            }

        });

    }

    private void scheduleHeartbeat() {
        mHandler.postDelayed(myTask, 1000);
    }

    private void cancelHeartbeat() {
        mHandler.removeCallbacks(myTask);
    }

    public void joinRoom(String room) {
        try {
            JSONObject j = new JSONObject();
            j.put("action", "join");
            j.put("room", room);
            Log.v(TAG, "joining room: " + room);
            send(j);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private char randomCharacterFromDictionary() {
        int rand = (int) (Math.random() * dictionary.length());
        return dictionary.charAt(rand);
    }

    private String randomStringOfLength(int length) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < length; i++) {
            s.append(randomCharacterFromDictionary());
        }
        return s.toString();
    }

    // "ws://cine-io-signaling.herokuapp.com/primus/211/b9__ftym/websocket"
    private String generateSignalingUrl() {
        Random r = new Random();
        int server = r.nextInt(1000);
        String connId = randomStringOfLength(8);
        return baseUrl + "/" + server + "/" + connId + "/websocket";
    }

    private void sendToWebsocket(String data) {
        data = "[\"" + data + "\"]";
        Log.v(TAG, data);
        webSocket.send(data);
    }

    public void onPause() {
        cancelHeartbeat();
        webSocket.end();
    }

    public void send(final JSONObject j) {
        try {
            j.put("source", "android");
            Log.d(TAG, "SENDING STUFF: " + j.toString());
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        webSocket.send(j.toString(4));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendToOtherSpark(String mOtherClientSparkId, JSONObject j) {
        try {
            j.put("sparkId", mOtherClientSparkId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        send(j);
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
