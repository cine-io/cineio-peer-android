package io.cine.androidwebsocket;

import android.app.Activity;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by thomas on 9/15/14.
 */
public class SignalingConnection {
    private static final String TAG = "SignalingConnection";
    private final Activity activity;
    private final String baseUrl = "http://192.168.1.114:8888/primus";
    private final Primus primus;
    private String apiKey;
    private String identity;
    private boolean mWebsocketOpen;

//    we always push messages to a pendingMessagesToProcess
//    that way we can ensure that we're actually ready to process messages.
//    we could add hooks in myActivity to tell the signaling connection to wait to process messages
//    currently we're waiting until we have websocket ready to process any messages.
    private Queue<CineMessage> pendingMessagesToProcess;
    private PeerConnectionsManager mPeerConnectionsManager;

    private SignalingConnection(Activity activity) {
        this.activity = activity;
        this.mWebsocketOpen = false;
        primus = Primus.connect(activity, baseUrl);
        pendingMessagesToProcess = new LinkedList<CineMessage>();

        setOpenCallback(new Primus.PrimusOpenCallback() {

            @Override
            public void onOpen() {
                mWebsocketOpen = true;
                processPendingMessages();
            }
        });

        setDataCallback(new Primus.PrimusDataCallback() {

            @Override
            public void onData(JSONObject response) {
                Log.v(TAG, "parsed response");
                newMessage(new CineMessage(response));
            }
        });

    }

    public static SignalingConnection connect(Activity activity) {
        return new SignalingConnection(activity);
    }

    public void init(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setDataCallback(Primus.PrimusDataCallback callback) {
        primus.setDataCallback(callback);
    }

    public void setOpenCallback(Primus.PrimusOpenCallback callback) {
        primus.setOpenCallback(callback);
    }


    public void identify(String identity) {
        try {
            this.identity = identity;
            JSONObject j = new JSONObject();
            j.put("action", "identify");
            j.put("identity", identity);
            Log.v(TAG, "identifying: " + identity);
            send(j);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void joinRoom(String room) {
        try {
            JSONObject j = new JSONObject();
            j.put("action", "join");
            j.put("room", room);
            Log.v(TAG, "joining room: " + room);
            send(j);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void call(String otherIdentity) {
        try {
            JSONObject j = new JSONObject();
            j.put("action", "call");
            j.put("identity", this.identity);
            j.put("otheridentity", otherIdentity);
            Log.v(TAG, "calling: " + identity);
            send(j);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void end() {
        primus.end();
    }

    private void send(JSONObject j) {
        try {
            j.put("source", "android");
            j.put("apikey", this.apiKey);
            primus.send(j);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendToOtherSpark(String mOtherClientSparkId, JSONObject j) {
        try {
            j.put("sparkId", mOtherClientSparkId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        send(j);
    }

    public void sendLocalDescription(String mOtherClientSparkId, SessionDescription localSdp) {
        JSONObject sdpJSON = new JSONObject();
        String type = localSdp.type.canonicalForm();
        try {
            sdpJSON.put("type", type);
            sdpJSON.put("sdp", localSdp.description);
            JSONObject json = new JSONObject();
            json.put(type, sdpJSON);
            json.put("action", type);
            sendToOtherSpark(mOtherClientSparkId, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendIceCandidate(String mOtherClientSparkId, IceCandidate candidate) {
        try {
            JSONObject candidateObject = new JSONObject();
            candidateObject.put("candidate", candidate.sdp);
            candidateObject.put("sdpMid", candidate.sdpMid);
            candidateObject.put("sdpMLineIndex", candidate.sdpMLineIndex);
            JSONObject j = new JSONObject();
            j.put("action", "ice");
            JSONObject candidateMiddleMan = new JSONObject();
            candidateMiddleMan.put("candidate", candidateObject);
            j.put("candidate", candidateMiddleMan);

            sendToOtherSpark(mOtherClientSparkId, j);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void gotNewIce(CineMessage response) {
        String otherClientSparkId = response.getString("sparkId");

        mPeerConnectionsManager.newIce(otherClientSparkId, response.getJSONObject("candidate"));
    }

    private void gotNewOffer(CineMessage response) {
        String otherClientSparkId = response.getString("sparkId");
        mPeerConnectionsManager.newOffer(otherClientSparkId, response.getJSONObject("offer"));
    }

    private void gotNewAnswer(CineMessage response) {
        String otherClientSparkId = response.getString("sparkId");
        mPeerConnectionsManager.newAnswer(otherClientSparkId, response.getJSONObject("answer"));
    }

    private void memberLeft(CineMessage response) {
        String otherClientSparkId = response.getString("sparkId");
        mPeerConnectionsManager.memberLeft(otherClientSparkId);
    }

    //    "{\"action\":\"member\",\"room\":\"hello\",\"sparkId\":\"5fa684d5-708b-4674-b548-b8b12011aa02\"}"
    private void gotNewMember(CineMessage response) {
        String otherClientSparkId = response.getString("sparkId");
        mPeerConnectionsManager.newMember(otherClientSparkId);
    }

    private void handleCall(CineMessage response) {
        joinRoom(response.getString("room"));
    }


    private void gotAllServers(CineMessage response) {
        try {
            JSONArray allServers = response.getJSONArray("data");
            for (int i = 0; i < allServers.length(); i++) {
                JSONObject iceServerData = allServers.getJSONObject(i);
                String url = iceServerData.getString("url");
                if (url.startsWith("stun:")) {
                    Log.v(TAG, "Addding ice stun server: " + url);
                    mPeerConnectionsManager.addStunServer(url);
                } else {
                    String credential = iceServerData.getString("credential");
                    String username = iceServerData.getString("username");
                    Log.v(TAG, "Addding ice turn server: " + url);
//                    url, credential, username
                    mPeerConnectionsManager.addTurnServer(url, username, credential);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void newMessage(CineMessage message) {
        pendingMessagesToProcess.add(message);
        processPendingMessages();
    }

    private void processPendingMessages() {
        if (!mWebsocketOpen) {
            Log.v(TAG, "websocket not open, not handling messages");
            return;
        }
        while (!pendingMessagesToProcess.isEmpty()) {
            CineMessage message = pendingMessagesToProcess.remove();
            Log.v(TAG, "HANDLING CINE MESSAGE: " + message.getAction());
            actuallyProcessMessage(message);
        }
    }

    private void actuallyProcessMessage(CineMessage message) {
        String action = message.getAction();
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


    public void setPeerConnectionsManager(PeerConnectionsManager mPeerConnectionsManager) {
        this.mPeerConnectionsManager = mPeerConnectionsManager;
    }
}