package io.cine.cineiopeerclientexampleapp.exampleapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;

import io.cine.peerclient.CinePeerClient;
import io.cine.peerclient.CinePeerClientConfig;
import io.cine.peerclient.CinePeerRenderer;
import io.cine.peerclient.CinePeerView;
import io.cine.peerclient.MediaStreamHolder;
import io.cine.peerclient.VideoRendererBackend;
import io.cine.peerclient.receiver.PlayUnavailableException;

public class MainActivity extends Activity implements CinePeerRenderer {
        private static final String TAG = "MainActivity";

        private CinePeerView vsv;
        private CinePeerClient cinePeerClient;
//        private VideoRenderer localRender;
    private ArrayList<MediaStreamHolder> mediaStreams;

//        private HashMap<MediaStream, VideoRenderer> remoteRenderers;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.v(TAG, "STARTING 2");
            startWakeLock();
            connectToCine();
            prepareLayout();
            cinePeerClient.startCameraAndMicrophone();
            cinePeerClient.joinRoom("hello");
        }

    private void startWakeLock() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
        protected void onNewIntent(Intent intent) {
            cinePeerClient.newIntent(intent);
        }

        @Override
        protected void onPause() {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            cinePeerClient.end();
            super.onPause();
        }

        private void connectToCine() {
            CinePeerClientConfig config = new CinePeerClientConfig("0b519f759096c48bf455941a02cf2c90", this);
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
            mediaStreams = new ArrayList<MediaStreamHolder>();
            vsv = cinePeerClient.createView();
            setContentView(vsv);
            rerenderLayout();
        }

    private MediaStreamHolder mediaStreamHolderFromStream(MediaStream stream){
        MediaStreamHolder mToReturn = null;
        for (MediaStreamHolder m : mediaStreams) {
            if (m.getStream().equals(stream)){
                mToReturn = m;
            }
        }
        return mToReturn;
    }

    private void cleanupMediaHolder(MediaStreamHolder mToRemove) {
        MediaStream stream = mToRemove.getStream();
        VideoRenderer renderer = mToRemove.getRenderer();
        if (renderer != null){
            Log.v(TAG, "REMOVING RENDERER FROM VIDEO TRACK");
            VideoTrack track = stream.videoTracks.getFirst();
            if (track != null){
                Log.v(TAG, "GOT TRACK");
            } else{
                Log.v(TAG, "TRACK IS NULL");
            }
//            Log.v(TAG, "CLOSED Track state: "+ track.state());
//            track.removeRenderer(renderer);
            Log.v(TAG, "REMOVING GUI FROM BACKEND");
            VideoRendererBackend.removeGui(renderer);
            Log.v(TAG, "REMOVED GUI FROM BACKEND");

//                renderer.dispose();
        }

    }


    private void renderVideo(MediaStream stream, boolean big) {
        int x;
        int y;
        int width;
        int height;
        if(big){
            x = 0;
            y = 0;
            width = 100;
            height = 100;
        }else{
            x = 75;
            y = 5;
            width = 25;
            height = 25;
        }
        MediaStreamHolder h = mediaStreamHolderFromStream(stream);

        VideoRenderer renderer;
        if(h != null){
            renderer = h.getRenderer();
            if (renderer != null){
                stream.videoTracks.get(0).removeRenderer(renderer);
                VideoRendererBackend.removeGui(renderer);
                renderer.dispose();
            }
        }
        try {
            if(big){
                Log.v(TAG, "CREAGING BIG GUI");
            } else{
                Log.v(TAG, "CREAGING LITTLE GUI");
            }
            renderer = VideoRendererBackend.createGui(x, y, width, height);

            h.addRenderer(renderer);

            VideoTrack v = stream.videoTracks.getFirst();
            Log.v(TAG, "ACTIVE Track state: "+ v.state());
            v.addRenderer(renderer);

        } catch (Exception e) {
            Log.v(TAG, "GOT EXCEPTION");
            e.printStackTrace();
        }
    }

    private void rerenderLayout(){

        Integer iterate1;

        for (iterate1 = 0; iterate1 < mediaStreams.size(); iterate1++) {
            MediaStreamHolder streamHolder = mediaStreams.get(iterate1);
            if (iterate1.equals(mediaStreams.size() - 1)) {
                Log.v(TAG, "BIG (" + iterate1 + "/" + mediaStreams.size()+")");
                MediaStream stream = streamHolder.getStream();
                renderVideo(stream, true);
            }
        }
        Log.v(TAG, "Number of streams: "+ iterate1 + ", think there are: "+mediaStreams.size());

        for (iterate1 = 0; iterate1 < mediaStreams.size(); iterate1++) {
            MediaStreamHolder streamHolder = mediaStreams.get(iterate1);
            if (!iterate1.equals(mediaStreams.size() - 1)){
                Log.v(TAG, "LITTLE (" + iterate1 + "/" + mediaStreams.size()+")");
                MediaStream stream = streamHolder.getStream();
                renderVideo(stream, false);
            }
        }
    }

    @Override
    public void mediaAdded(MediaStream stream, boolean local) {
        if (local){
            Log.v(TAG, "adding local renderer");
        }else {
            Log.v(TAG, "adding remote renderer");
        }
        Log.v(TAG, "hashCode: "+ stream.hashCode());
        MediaStreamHolder m = new MediaStreamHolder(stream);
        mediaStreams.add(m);
        rerenderLayout();
    }

    @Override
    public void mediaRemoved(MediaStream stream, boolean local) {
        if (local){
            Log.v(TAG, "removing local renderer");
        }else {
            Log.v(TAG, "removing remote renderer");
        }
        MediaStreamHolder mToRemove = mediaStreamHolderFromStream(stream);
        Log.v(TAG, "MEDIA STREAMS BEFORE REMOVED: "+mediaStreams.size());
        if(mToRemove != null) {
            cleanupMediaHolder(mToRemove);
            mediaStreams.remove(mToRemove);
        }
        Log.v(TAG, "MEDIA STREAMS AFTER REMOVED: "+mediaStreams.size());
        rerenderLayout();
    }

    @Override
    public void peerData() {

    }

    @Override
    public void onCall() {

    }

//        @Override
//        public VideoRenderer.Callbacks getLocalRenderer() {
//            return localRender;
//        }
//
//        @Override
//        public VideoRenderer.Callbacks getRemoteRenderer() {
//            return remoteRender;
//        }



}
