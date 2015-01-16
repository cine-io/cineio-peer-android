package io.cine.cineiopeerclientexampleapp.exampleapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.ArrayList;

import io.cine.peerclient.Call;
import io.cine.peerclient.CinePeerClient;
import io.cine.peerclient.CinePeerClientConfig;
import io.cine.peerclient.CinePeerRenderer;
import io.cine.peerclient.CinePeerView;
import io.cine.peerclient.receiver.PlayUnavailableException;

public class MainActivity extends Activity implements CinePeerRenderer {
    private static final String TAG = "MainActivity";
    private static final String PUBLIC_KEY = "CINE_IO_PUBLIC_KEY";

    private CinePeerView vsv;
    private CinePeerClient cinePeerClient;
    private ArrayList<MediaStreamAndRenderer> mediaStreamAndRenderers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectToCine();
        prepareLayout();
        cinePeerClient.startMediaStream();
            cinePeerClient.joinRoom("hello");
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
        try {
            cinePeerClient = CinePeerClient.init(config);
        } catch (PlayUnavailableException e) {
            Log.v(TAG, "Google Play is unavailable");
            e.printStackTrace();
        }
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
        return VideoRendererGui.create(x,y, width, height);
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
        VideoRendererGui.setView(vsv);
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
    public void peerData(JSONObject object) {
        try {
            Toast.makeText(this, object.getString("message"), Toast.LENGTH_SHORT);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCall(Call call) {

    }
}
