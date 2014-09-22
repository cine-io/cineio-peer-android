package io.cine.androidwebsocket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import io.cine.androidwebsocket.receiver.PlayUnavailableException;


public class MyActivity extends Activity implements CinePeerRenderer{
    private static final String TAG = "AndroidWebsocketTest";

    private CinePeerView vsv;
    private boolean factoryStaticInitialized;
    private CinePeerClient cinePeerClient;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!factoryStaticInitialized) {
            RTCHelper.abortUnless(PeerConnectionFactory.initializeAndroidGlobals(
                            this, true, true),
                    "Failed to initializeAndroidGlobals"
            );
            factoryStaticInitialized = true;
        }
        connectToCine();
        prepareLayout();
        startMediaStream();
    }


    // You need to do the Play Services APK check here too.
//    @Override
//    protected void onResume() {
//        super.onResume();
//    }

    protected void onNewIntent(Intent intent) {
        cinePeerClient.newIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cinePeerClient.end();
    }

    private void connectToCine() {
        CinePeerClientConfig config = new CinePeerClientConfig("TEST_API_KEY", this);
        try {
            cinePeerClient = CinePeerClient.init(config);
        } catch (PlayUnavailableException e) {
            Log.v(TAG, "Google Play is unavailable");
            e.printStackTrace();
        }
    }

    private void prepareLayout() {
        vsv = new CinePeerView(this);

        remoteRender = VideoRendererGui.create(0, 0, 100, 100);
        localRender = VideoRendererGui.create(70, 5, 25, 25);
        setContentView(vsv);
    }

    @Override
    public VideoRenderer.Callbacks getLocalRenderer(){
        return localRender;
    }

    @Override
    public VideoRenderer.Callbacks getRemoteRenderer() {
        return remoteRender;
    }


    private void startMediaStream() {
        cinePeerClient.startMediaStream();
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
