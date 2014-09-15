package io.cine.androidwebsocket;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

public class VideoActivity extends Activity {

    private static final String TAG = "VideoActivity";

    private AppRTCGLView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private MediaConstraints sdpMediaConstraints;
    private Toast logToast;
    private PeerConnectionFactory factory;
    private VideoSource videoSource;
    private boolean videoSourceStopped;
    private boolean factoryStaticInitialized;


    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        vsv.updateDisplaySize(displaySize);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(
                new UnhandledExceptionHandler(this));

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(displaySize);

        vsv = new AppRTCGLView(this, displaySize);
        VideoRendererGui.setView(vsv);
        remoteRender = VideoRendererGui.create(0, 0, 100, 100);
        localRender = VideoRendererGui.create(70, 5, 25, 25);
        setContentView(vsv);


        if (!factoryStaticInitialized) {
            RTCHelper.abortUnless(PeerConnectionFactory.initializeAndroidGlobals(
                            this, true, true),
                    "Failed to initializeAndroidGlobals");
            factoryStaticInitialized = true;
        }

        AudioManager audioManager =
                ((AudioManager) getSystemService(AUDIO_SERVICE));
        // TODO(fischman): figure out how to do this Right(tm) and remove the
        // suppression.
        @SuppressWarnings("deprecation")
        boolean isWiredHeadsetOn = audioManager.isWiredHeadsetOn();
        audioManager.setMode(isWiredHeadsetOn ?
                AudioManager.MODE_IN_CALL : AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(!isWiredHeadsetOn);

        sdpMediaConstraints = new MediaConstraints();
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));
        factory = new PeerConnectionFactory();

        Log.d(TAG, "Creating local video source...");
        MediaStream lMS = factory.createLocalMediaStream("ARDAMS");
//        if (appRtcClient.videoConstraints() != null) {
            VideoCapturer capturer = getVideoCapturer();
        MediaConstraints blankMediaConstraints = new MediaConstraints();
            videoSource = factory.createVideoSource(
                    capturer, blankMediaConstraints);
            VideoTrack videoTrack =
                    factory.createVideoTrack("ARDAMSv0", videoSource);
            videoTrack.addRenderer(new VideoRenderer(localRender));
            lMS.addTrack(videoTrack);
//        }
//        if (appRtcClient.audioConstraints() != null) {
            lMS.addTrack(factory.createAudioTrack(
                    "ARDAMSa0",
                    factory.createAudioSource(blankMediaConstraints)));
//        }
    }

    // Cycle through likely device names for the camera and return the first
    // capturer that works, or crash if none do.
    private VideoCapturer getVideoCapturer() {
        String[] cameraFacing = { "front", "back" };
        int[] cameraIndex = { 0, 1 };
        int[] cameraOrientation = { 0, 90, 180, 270 };
        for (String facing : cameraFacing) {
            for (int index : cameraIndex) {
                for (int orientation : cameraOrientation) {
                    String name = "Camera " + index + ", Facing " + facing +
                            ", Orientation " + orientation;
                    VideoCapturer capturer = VideoCapturer.create(name);
                    if (capturer != null) {
                        Log.d(TAG, "Using camera: " + name);
                        return capturer;
                    }
                }
            }
        }
        throw new RuntimeException("Failed to open capturer");
    }

    @Override
    public void onPause() {
        super.onPause();
        vsv.onPause();
        if (videoSource != null) {
            videoSource.stop();
            videoSourceStopped = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        vsv.onResume();
        if (videoSource != null && videoSourceStopped) {
            videoSource.restart();
            videoSourceStopped = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.video, menu);
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
