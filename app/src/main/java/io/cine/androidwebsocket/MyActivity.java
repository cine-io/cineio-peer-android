package io.cine.androidwebsocket;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.LinkedList;
import java.util.Queue;

import io.cine.androidwebsocket.receiver.GcmRegisterer;
import io.cine.androidwebsocket.receiver.PlayUnavailableException;


public class MyActivity extends Activity {
    private static final String TAG = "AndroidWebsocketTest";

    private StartRTC mStartRTC;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private AppRTCGLView vsv;
    private boolean factoryStaticInitialized;
    private MediaStream lMS;
    private SignalingConnection signalingConnection;
    private boolean receivedAllServer;
    private VideoSource videoSource;
    private Queue<CineMessage> onConnectActions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        receivedAllServer = false;
        if (!factoryStaticInitialized) {
            RTCHelper.abortUnless(PeerConnectionFactory.initializeAndroidGlobals(
                            this, true, true),
                    "Failed to initializeAndroidGlobals"
            );
            factoryStaticInitialized = true;
        }
        try {
            GcmRegisterer.registerWithCine(this);
        } catch (PlayUnavailableException e) {
            Log.v(TAG, "Google Play is unavailable");
            e.printStackTrace();
        }
        onConnectActions = new LinkedList<CineMessage>();
        connect();
        startRTC();
        prepareLayout();
        startVideo();
    }

//    // You need to do the Play Services APK check here too.
//    @Override
//    protected void onResume() {
//        super.onResume();
//    }

    protected void onNewIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.v(TAG, "SOME EXTRAS" + extras.toString());
            onConnectActions.add(new CineMessage(extras));
            processPendingMessages();
//            connect();
        } else {
            Log.v(TAG, "NO extras");
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        signalingConnection.end();
    }

    private void processPendingMessages() {
        signalingConnection.joinRoom("hello");
        while (!onConnectActions.isEmpty()) {
            CineMessage message = onConnectActions.remove();
            Log.v(TAG, "HANDLING CINE MESSAGE: " + message.getAction());
            handleCineMessage(message);
        }
    }

    private void startRTC() {
        mStartRTC = new StartRTC(this, signalingConnection);
    }

    private void connect() {
//        if (signalingConnection != null) {
//            signalingConnection.end();
//            receivedAllServer = false;
//        }
        signalingConnection = SignalingConnection.connect(this);
        signalingConnection.init("TEST_API_KEY");
        signalingConnection.setOpenCallback(new Primus.PrimusOpenCallback() {
            @Override
            public void onOpen() {
//                processPendingMessages();
            }
        });
        signalingConnection.setDataCallback(new Primus.PrimusDataCallback() {

            @Override
            public void onData(JSONObject response) {
                Log.v(TAG, "parsed response");
                handleCineMessage(new CineMessage(response));
            }
        });
    }

    private void handleCineMessage(CineMessage message) {
        String action = message.getString("action");
        Log.v(TAG, "action is: " + action);
        if (action.equals("allservers")) {
            Log.v(TAG, "GOT ALL SERVERS");
            gotAllServers(message);
        } else if (action.equals("member")) {
            Log.v(TAG, "GOT new member");
            gotNewMember(message);
        } else if (action.equals("offer")) {
            Log.v(TAG, "GOT new offer");
            gotNewOffer(message);
        } else if (action.equals("answer")) {
            Log.v(TAG, "GOT new answer");
            gotNewAnswer(message);
        } else if (action.equals("ice")) {
            Log.v(TAG, "GOT new ice");
            gotNewIce(message);
        } else if (action.equals("leave")) {
            Log.v(TAG, "GOT new leave");
            memberLeft(message);
        } else if (action.equals("incomingcall")) {
            Log.v(TAG, "GOT new incoming call");
            handleCall(message);
        } else {
            Log.v(TAG, "Unknown action");
        }
    }

    private void handleCall(CineMessage response) {
        signalingConnection.joinRoom(response.getString("room"));
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


    private void startVideo() {
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
        PeerConnectionFactory factory = StartRTC.getFactory();
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
        mStartRTC.setMediaStream(lMS);

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

    private void gotNewIce(CineMessage response) {
        String otherClientSparkId = response.getString("sparkId");

        mStartRTC.newIce(otherClientSparkId, response.getJSONObject("candidate"));
    }

    private void gotNewOffer(CineMessage response) {
        String otherClientSparkId = response.getString("sparkId");
        mStartRTC.newOffer(otherClientSparkId, response.getJSONObject("offer"));
    }

    private void gotNewAnswer(CineMessage response) {
        String otherClientSparkId = response.getString("sparkId");
        mStartRTC.newAnswer(otherClientSparkId, response.getJSONObject("answer"));
    }

    private void memberLeft(CineMessage response) {
        String otherClientSparkId = response.getString("sparkId");
        mStartRTC.memberLeft(otherClientSparkId);
    }

    //    "{\"action\":\"member\",\"room\":\"hello\",\"sparkId\":\"5fa684d5-708b-4674-b548-b8b12011aa02\"}"
    private void gotNewMember(CineMessage response) {
        String otherClientSparkId = response.getString("sparkId");
        mStartRTC.newMember(otherClientSparkId);
    }


    private void gotAllServers(CineMessage response) {
        if (receivedAllServer) {
            return;
        }
        receivedAllServer = true;
        try {
            JSONArray allServers = response.getJSONArray("data");
            for (int i = 0; i < allServers.length(); i++) {
                JSONObject iceServerData = allServers.getJSONObject(i);
                String url = iceServerData.getString("url");
                if (url.startsWith("stun:")) {
                    Log.v(TAG, "Addding ice stun server: " + url);
                    mStartRTC.addStunServer(url);
                } else {
                    String credential = iceServerData.getString("credential");
                    String username = iceServerData.getString("username");
                    Log.v(TAG, "Addding ice turn server: " + url);
//                    url, credential, username
                    mStartRTC.addTurnServer(url, username, credential);
                }
            }
            processPendingMessages();
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
