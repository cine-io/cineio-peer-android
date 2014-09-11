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

import org.json.JSONException;
import org.json.JSONObject;


public class MyActivity extends Activity {
    private static final String TAG = "AndroidWebsocketTest";

    private Handler mHandler;
    Runnable myTask = new Runnable() {
        @Override
        public void run() {
            mWebSocket.send("primus::ping::"+ System.currentTimeMillis());
            mHandler.postDelayed(this, 25000);
        }
    };
    private WebSocket mWebSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        mHandler = new Handler();
        go();
    }

    private void go() {
        String url = "ws://cine-io-signaling.herokuapp.com/primus/211/b9__ftym/websocket";
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
                mHandler.postDelayed(myTask, 1000);

                JSONObject j = new JSONObject();
                try {
                    j.put("some", "data");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
                        Log.v(TAG, "got string" + s);

                        System.out.println("I got a string: " + s);
                    }
                });

                webSocket.send(j.toString());

            }
        });

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
