package io.cine.cineiopeerclientexampleapp.exampleapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.ArrayList;

import io.cine.peerclient.Call;
import io.cine.peerclient.CallHandler;
import io.cine.peerclient.CineMessage;
import io.cine.peerclient.CinePeerCallback;
import io.cine.peerclient.CinePeerClient;
import io.cine.peerclient.CinePeerClientConfig;
import io.cine.peerclient.CinePeerView;

public class MainActivity extends Activity implements CinePeerCallback {
    private static final String TAG = "MainActivity";
    private static final String PUBLIC_KEY = "CINE_IO_PUBLIC_KEY";
    private static final String SECRET_KEY = "CINE_IO_SECRET_KEY"; //Only used for identifying
    private static final String EXAMPLE_ROOM_NAME = "hello";

    private CinePeerView vsv;
    private CinePeerClient cinePeerClient;
    private ArrayList<MediaStreamAndRenderer> mediaStreamAndRenderers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectToCine();
        prepareLayout();
        cinePeerClient.startMediaStream();
        cinePeerClient.joinRoom(EXAMPLE_ROOM_NAME);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        cinePeerClient.newIntent(intent);
    }

    @Override
    protected void onPause() {
        cinePeerClient.end();
        super.onPause();
    }

    private void connectToCine() {
        CinePeerClientConfig config = new CinePeerClientConfig(PUBLIC_KEY, this);
        config.setVideo(true);
        config.setAudio(true);
        cinePeerClient = CinePeerClient.init(config);
    }

    private void prepareLayout() {
        setContentView(R.layout.activity_main);
        mediaStreamAndRenderers = new ArrayList<MediaStreamAndRenderer>();

        resetView();
    }


    private VideoRenderer.Callbacks makeRenderer(int count) {
        int width = 25;
        int height = 25;
        int x = count * 25;
        int y = 5;

        return VideoRendererGui.create(x,y, width, height, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
    }

    private void recalculateLayout() {
        for (MediaStreamAndRenderer msr : this.mediaStreamAndRenderers) {
            clearMediaStream(msr);
        }

        resetView();

        for(int i = 0; i < this.mediaStreamAndRenderers.size(); i++){
            MediaStreamAndRenderer msr = this.mediaStreamAndRenderers.get(i);
            showMediaStream(msr, i);
        }
    }

    private void resetView() {
        LinearLayout l = (LinearLayout) findViewById(R.id.theLinearLayout);
        if (vsv != null) {
            l.removeView(vsv);
        }
        vsv = cinePeerClient.createView();
        Runnable eglContextReadyCallback = null;
        VideoRendererGui.setView(vsv, eglContextReadyCallback);
        l.addView(vsv);
    }

    private void clearMediaStream(MediaStreamAndRenderer msr) {
        msr.removeVideoRenderer();
    }

    private void showMediaStream(MediaStreamAndRenderer msr, int index) {
        msr.setVideoRenderer(new VideoRenderer(makeRenderer(index)));
    }

    public void mediaAdded(MediaStream stream, boolean local) {
        MediaStreamAndRenderer msr = new MediaStreamAndRenderer(stream, local);
        mediaStreamAndRenderers.add(msr);
        recalculateLayout();
    }

    @Override
    public void mediaRemoved(MediaStream stream, boolean local) {
        MediaStreamAndRenderer toDelete = null;
        for(MediaStreamAndRenderer msr: this.mediaStreamAndRenderers) {
            if (msr.getMediaStream().equals(stream)) {
                toDelete = msr;
            }
        }
        // do not clear out the video stream renderer, it will segfault
        mediaStreamAndRenderers.remove(toDelete);
        recalculateLayout();
    }

    @Override
    public void onCall(Call call) {
        final Context self = this;
        call.setCallHandler(new CallHandler() {
            @Override
            public void onCancel(String identity) {
                String text = identity + " cancelled.";
                Toast.makeText(self, text, Toast.LENGTH_SHORT);
            }

            @Override
            public void onReject(String identity) {
                String text = identity + " rejected.";
                Toast.makeText(self, text, Toast.LENGTH_SHORT);
            }
        });
        Log.v(TAG, "ANSWERING");
        call.answer();
    }

    @Override
    public void onError(CineMessage message) {
    // these messages are errors from the signaling server

    }
}
