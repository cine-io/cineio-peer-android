package io.cine.androidwebsocket;

import android.app.Activity;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

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

import java.util.Random;


public class MyActivity extends Activity {
    private static final String TAG = "AndroidWebsocketTest";

    private Handler mHandler;
    Runnable myTask = new Runnable() {
        @Override
        public void run() {
            String ping = "primus::ping::" + System.currentTimeMillis();
//            Log.v(TAG, "SENDING PING - " + ping);
//            Log.v(TAG, mWebSocket.isOpen() ? "socket open" : "socket closed");
            sendToWebsocket(ping);
            mHandler.postDelayed(this, 10000);
        }
    };
    private WebSocket mWebSocket;
    private StartRTC mStartRTC;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private AppRTCGLView vsv;
    private boolean factoryStaticInitialized;
    private MediaStream lMS;

    // Poor-man's assert(): die with |msg| unless |condition| is true.
    private static void abortUnless(boolean condition, String msg) {
        if (!condition) {
            throw new RuntimeException(msg);
        }
    }

    public void addStream(MediaStream stream){
        stream.videoTracks.get(0).addRenderer(
                new VideoRenderer(remoteRender));
    }

    private void sendToWebsocket(String data){
        data = "[\"" + data + "\"]";
        Log.v(TAG, data);
        mWebSocket.send(data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(myTask);
        mWebSocket.end();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        if (!factoryStaticInitialized) {
            abortUnless(PeerConnectionFactory.initializeAndroidGlobals(
                            this, true, true),
                    "Failed to initializeAndroidGlobals");
            factoryStaticInitialized = true;
        }
        mStartRTC = new StartRTC(this);
        prepareLayout();
        startVideo();
        go();
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


    private Toast logToast;

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    private VideoSource videoSource;

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

        logAndToast("Creating local video source...");
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
        String[] cameraFacing = { "front", "back" };
        int[] cameraIndex = { 0, 1 };
        int[] cameraOrientation = { 0, 90, 180, 270 };
        for (String facing : cameraFacing) {
            Log.v(TAG, "facing: "+facing);
            for (int index : cameraIndex) {
                Log.v(TAG, "index: "+index);
                for (int orientation : cameraOrientation) {
                    Log.v(TAG, "orientation: "+orientation);
                    String name = "Camera " + index + ", Facing " + facing +
                            ", Orientation " + orientation;
                    Log.v(TAG, "name: " + name);
                    VideoCapturer capturer = VideoCapturer.create(name);
                    Log.v(TAG, "got capturer");
                    if (capturer != null) {
                        logAndToast("Using camera: " + name);
                        return capturer;
                    }
                }
            }
        }
        throw new RuntimeException("Failed to open capturer");
    }

    private char randomCharacterFromDictionary(){
        String dictionary = "abcdefghijklmnopqrstuvwxyz0123456789_";
        int rand = (int)(Math.random() * dictionary.length());
        return dictionary.charAt(rand);
    }

    private String randomStringOfLength(int length){
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < length; i++){
            s.append(randomCharacterFromDictionary());
        }
        return s.toString();
    }

    // "ws://cine-io-signaling.herokuapp.com/primus/211/b9__ftym/websocket"
    private String getSignalingUrl(){
        Random r = new Random();
        int server = r.nextInt(1000);
        String connId = randomStringOfLength(8);
        return "http://192.168.1.114:8888/primus/"+server+"/"+connId+"/websocket";
    }

    private void go() {
        String url = getSignalingUrl();
        Log.v(TAG, url);
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
                mStartRTC.setSignalingConnection(webSocket);
                mHandler.postDelayed(myTask, 1000);

                webSocket.setDataCallback(new DataCallback() {
                    @Override
                    public void onDataAvailable(DataEmitter dataEmitter, ByteBufferList byteBufferList) {
                        Log.v(TAG, "I got some bytes!");
                        // note that this data has been read
                        byteBufferList.recycle();
                    }
                });
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
                        Log.v(TAG, "got string: " + s);
                        try {
                            JSONObject response;
                            if (s.startsWith("o")){
                                handleOpen();
                                return;
                            }
                            else if (s.startsWith("a")){
                                Log.v(TAG, "received array");
                                s = s.substring(1);
                                JSONArray r = new JSONArray(s);
                                Log.v(TAG, "parsed array");
                                response = new JSONObject(r.getString(0));
                            }else{
                                Log.v(TAG, "received something else");
                                return;
                            }
                            Log.v(TAG, "parsed response");
                            String action = response.getString("action");
                            Log.v(TAG, "action is: "+action);
                            if (action.equals("allservers")) {
                                Log.v(TAG, "GOT ALL SERVERS");
                                gotAllServers(response);
                            } else if (action.equals("member")){
                                Log.v(TAG, "GOT new member");
                                gotNewMember(response);
                            } else if (action.equals("offer")){
                                Log.v(TAG, "GOT new offer");
                                gotNewOffer(response);
                            } else if (action.equals("answer")){
                                Log.v(TAG, "GOT new answer");
                                gotNewAnswer(response);
                            } else if (action.equals("ice")){
                                Log.v(TAG, "GOT new ice");
                                gotNewIce(response);
                            } else {
                                Log.v(TAG, "Unknown action");
                            }
                        } catch (JSONException e) {
                            Log.v(TAG, "UNABLE TO PARSE RESPONSE");
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

    }

    private void gotNewIce(JSONObject response) {
        try {
            String otherClientSparkId = response.getString("sparkId");

            mStartRTC.newIce(otherClientSparkId, response.getJSONObject("candidate"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void gotNewOffer(JSONObject response) {
        try {
            String otherClientSparkId = response.getString("sparkId");
            mStartRTC.newOffer(otherClientSparkId, response.getJSONObject("offer"));
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private void gotNewAnswer(JSONObject response) {

    }

    //    "{\"action\":\"member\",\"room\":\"hello\",\"sparkId\":\"5fa684d5-708b-4674-b548-b8b12011aa02\"}"
    private void gotNewMember(JSONObject response) {
        try {
            String otherClientSparkId = response.getString("sparkId");
            mStartRTC.newMember(otherClientSparkId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }



    private void handleOpen() {
        joinRoom("hello");
    }

    private void joinRoom(String room) {
        try {
            JSONObject j = new JSONObject();
            j.put("action", "join");
            j.put("room", room);
            mWebSocket.send(j.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void gotAllServers(JSONObject response) {
        try {
            JSONArray allServers = response.getJSONArray("data");
            for (int i = 0; i < allServers.length(); i++){
                JSONObject iceServerData = (JSONObject) allServers.get(i);
                String url = (String) iceServerData.get("url");
                Log.v(TAG, url);
                if (url.startsWith("stun:")){
                    mStartRTC.addIceServer(url);
                }else{
                    Log.v(TAG, "did not add ice server");
                }
            }
            mStartRTC.start();
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
