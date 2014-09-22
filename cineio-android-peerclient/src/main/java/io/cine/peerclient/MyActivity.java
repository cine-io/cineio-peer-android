package io.cine.peerclient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import io.cine.peerclient.receiver.PlayUnavailableException;


public class MyActivity extends Activity implements CinePeerRenderer {
    private static final String TAG = "MyActivity";

    private CinePeerView vsv;
    private CinePeerClient cinePeerClient;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectToCine();
        prepareLayout();
        cinePeerClient.startMediaStream();
//        cinePeerClient.joinRoom("hello");
    }

    @Override
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
        config.setVideo(true);
        config.setAudio(true);
        try {
            cinePeerClient = CinePeerClient.init(config);
        } catch (PlayUnavailableException e) {
            Log.v(TAG, "Google Play is unavailable");
            e.printStackTrace();
        }
    }

    private void prepareLayout() {
        vsv = cinePeerClient.createView();

        remoteRender = VideoRendererGui.create(0, 0, 100, 100);
        localRender = VideoRendererGui.create(70, 5, 25, 25);
        setContentView(vsv);
    }

    @Override
    public VideoRenderer.Callbacks getLocalRenderer() {
        return localRender;
    }

    @Override
    public VideoRenderer.Callbacks getRemoteRenderer() {
        return remoteRender;
    }

}
