package io.cine.androidwebsocket;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import io.cine.androidwebsocket.receiver.GcmRegisterer;
import io.cine.androidwebsocket.receiver.PlayUnavailableException;


public class MyActivity extends Activity {
    private static final String TAG = "AndroidWebsocketTest";

    private PeerConnectionsManager mPeerConnectionsManager;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private AppRTCGLView vsv;
    private boolean factoryStaticInitialized;
    private MediaStream lMS;
    private SignalingConnection mSignalingConnection;
    private VideoSource videoSource;

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
        registerWithCine();
        connectToCine();
        prepareLayout();
        startMediaStream();
    }

    private void registerWithCine() {
        try {
            GcmRegisterer.registerWithCine(this);
        } catch (PlayUnavailableException e) {
            Log.v(TAG, "Google Play is unavailable");
            e.printStackTrace();
        }

    }

    // You need to do the Play Services APK check here too.
    @Override
    protected void onResume() {
        super.onResume();
        registerWithCine();
    }

    protected void onNewIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.v(TAG, "SOME EXTRAS" + extras.toString());
            mSignalingConnection.newMessage(new CineMessage(extras));
        } else {
            Log.v(TAG, "NO extras");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSignalingConnection.end();
    }

    private void connectToCine() {
        mPeerConnectionsManager = new PeerConnectionsManager(this);
        mSignalingConnection = SignalingConnection.connect(this);
        mSignalingConnection.init("TEST_API_KEY");
        mPeerConnectionsManager.setSignalingConnection(mSignalingConnection);
        mSignalingConnection.setPeerConnectionsManager(mPeerConnectionsManager);
    }

    private void prepareLayout() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(displaySize);

        vsv = new AppRTCGLView(this, displaySize);
        VideoRendererGui.setView(vsv);

        remoteRender = VideoRendererGui.create(0, 0, 100, 100);
        localRender = VideoRendererGui.create(70, 5, 25, 25);
        setContentView(vsv);
    }


    private void startMediaStream() {
        AudioManager audioManager =
                ((AudioManager) getSystemService(AUDIO_SERVICE));
        // TODO(fischman): figure out how to do this Right(tm) and remove the
        // suppression.
        @SuppressWarnings("deprecation")
        boolean isWiredHeadsetOn = audioManager.isWiredHeadsetOn();
        audioManager.setMode(isWiredHeadsetOn ?
                AudioManager.MODE_IN_CALL : AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(!isWiredHeadsetOn);

        MediaConstraints constraints = new MediaConstraints();

        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));

        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

        Log.d(TAG, "Creating local video source...");
        PeerConnectionFactory factory = PeerConnectionsManager.getFactory();
        Log.v(TAG, "1");
        MediaConstraints blankMediaConstraints = new MediaConstraints();

        lMS = factory.createLocalMediaStream("ARDAMS");
        Log.v(TAG, "2");
        VideoCapturer capturer = getVideoCapturer();
        Log.v(TAG, "3");
        videoSource = factory.createVideoSource(capturer, blankMediaConstraints);
        Log.v(TAG, "4");
        VideoTrack videoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);
        Log.v(TAG, "5");
        videoTrack.addRenderer(new VideoRenderer(localRender));
        Log.v(TAG, "6");
        lMS.addTrack(videoTrack);
//        if (appRtcClient.audioConstraints() != null) {
        lMS.addTrack(factory.createAudioTrack(
                "ARDAMSa0",
                factory.createAudioSource(blankMediaConstraints)));
//        }
        mPeerConnectionsManager.setMediaStream(lMS);
    }

    // Cycle through likely device names for the camera and return the first
    // capturer that works, or crash if none do.
    private VideoCapturer getVideoCapturer() {
        String[] cameraFacing = {"front", "back"};
        int[] cameraIndex = {0, 1};
        int[] cameraOrientation = {0, 90, 180, 270};
        for (String facing : cameraFacing) {
            Log.v(TAG, "facing: " + facing);
            for (int index : cameraIndex) {
                Log.v(TAG, "index: " + index);
                for (int orientation : cameraOrientation) {
                    Log.v(TAG, "orientation: " + orientation);
                    String name = "Camera " + index + ", Facing " + facing +
                            ", Orientation " + orientation;
                    Log.v(TAG, "name: " + name);
                    VideoCapturer capturer = VideoCapturer.create(name);
                    Log.v(TAG, "got capturer");
                    if (capturer != null) {
                        Log.d(TAG, "Using camera: " + name);
                        return capturer;
                    }
                }
            }
        }
        throw new RuntimeException("Failed to open capturer");
    }

    public void addStream(final MediaStream stream) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stream.videoTracks.get(0).addRenderer(
                        new VideoRenderer(remoteRender));
            }
        });
    }

    public void removeStream(final MediaStream stream) {
        runOnUiThread(new Runnable() {
            public void run() {
                Log.d(TAG, "DISPOSING OF STREAM");
                // causes the app to crash
                // stream.dispose();
                Log.d(TAG, "DISPOSED OF STREAM");
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
