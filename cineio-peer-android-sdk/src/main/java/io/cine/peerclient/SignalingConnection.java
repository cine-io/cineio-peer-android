package io.cine.peerclient;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import io.cine.primus.Primus;

/**
 * Created by thomas on 9/15/14.
 */
public class SignalingConnection {
    private static final String TAG = "SignalingConnection";
    private final String CINE_SIGNALING_URL = "http://signaling.cine.io/primus";
    private final CinePeerClientConfig config;
    private final Primus primus;
    private final String uuid;
    private final HashMap<String, Call> calls;
    private final ArrayList<String> rooms;
    private String publicKey;
    private Identity identity;
    private boolean mWebsocketOpen;

    //    we always push messages to a pendingMessagesToProcess
//    that way we can ensure that we're actually ready to process messages.
//    we could add hooks in myActivity to tell the signaling connection to wait to process messages
//    currently we're waiting until we have websocket ready to process any messages.
    private Queue<CineMessage> pendingMessagesToProcess;
    private PeerConnectionsManager mPeerConnectionsManager;

    private SignalingConnection(CinePeerClientConfig config) {
        this.config = config;
        this.publicKey = config.getPublicKey();
        this.mWebsocketOpen = false;
        this.uuid = UUID.randomUUID().toString();
        primus = Primus.connect(config.getActivity(), CINE_SIGNALING_URL);
        pendingMessagesToProcess = new LinkedList<CineMessage>();
        this.calls = new HashMap<String, Call>();
        this.rooms = new ArrayList<String>();

        setOpenCallback(new Primus.PrimusOpenCallback() {

            @Override
            public void onOpen() {
                mWebsocketOpen = true;
                auth();
                sendIdentify();
                rejoinAllRooms();
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

    private void auth() {
        try {
            JSONObject j = new JSONObject();
            j.put("action", "auth");
            send(j);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static SignalingConnection connect(CinePeerClientConfig config) {
        return new SignalingConnection(config);
    }

    public void setDataCallback(Primus.PrimusDataCallback callback) {
        primus.setDataCallback(callback);
    }

    public void setOpenCallback(Primus.PrimusOpenCallback callback) {
        primus.setOpenCallback(callback);
    }

    public void identify(String identity, String signature, long timestamp) {
        this.identity = new Identity(identity, signature, timestamp);
        sendIdentify();
    }

    private void sendIdentify(){
        if(this.identity == null){
            return;
        }
        try {
        JSONObject j = new JSONObject();
        j.put("action", "identify");
        j.put("identity", this.identity.getIdentity());
        j.put("signature", this.identity.getSignature());
        j.put("timestamp", this.identity.getTimestamp());
        Log.v(TAG, "identifying: " + identity);
        send(j);
    } catch (JSONException e) {
        e.printStackTrace();
    }
    }

    private void rejoinAllRooms(){
        for(String room: this.rooms){
            joinRoom(room);
        }
    }

    public void joinRoom(String room) {
        rooms.add(room);
        try {
            JSONObject j = new JSONObject();
            j.put("action", "room-join");
            j.put("room", room);
            Log.v(TAG, "joining room: " + room);
            send(j);
        } catch (JSONException e) {
            rooms.remove(room);
            e.printStackTrace();
        }
    }

    public void leaveRoom(String room) {
        rooms.remove(room);
        try {
            JSONObject j = new JSONObject();
            j.put("action", "room-leave");
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
            j.put("otheridentity", otherIdentity);
            Log.v(TAG, "calling: " + otherIdentity);
            send(j);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void call(String otherIdentity, String room) {
        try {
            JSONObject j = new JSONObject();
            j.put("action", "call");
            j.put("room", room);
            j.put("otheridentity", otherIdentity);
            Log.v(TAG, "calling: " + otherIdentity);
            send(j);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void callCancel(String otherIdentity, String room) {
        try {
            JSONObject j = new JSONObject();
            j.put("action", "call-cancel");
            j.put("room", "room");
            j.put("otheridentity", otherIdentity);
            send(j);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void rejectCall(String room) {
        try {
            JSONObject j = new JSONObject();
            j.put("action", "reject-call");
            j.put("room", "room");
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
            j.put("client", "cineio-peer-android version-" + CinePeerClient.VERSION);
            j.put("publicKey", this.publicKey);
            j.put("uuid", this.uuid);
            if(this.identity != null){
                j.put("identity", this.identity.getIdentity());
            }
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
            json.put("action", "rtc-" + type); //rtc-offer, rtc-answer
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
            j.put("action", "rtc-ice");
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
        String otherClientSparkUUID = response.getString("sparkUUID");
        mPeerConnectionsManager.newIce(otherClientSparkUUID, otherClientSparkId, response.getJSONObject("candidate"));
    }

    private void gotNewOffer(CineMessage response) {
        String otherClientSparkId = response.getString("sparkId");
        String otherClientSparkUUID = response.getString("sparkUUID");
        mPeerConnectionsManager.newOffer(otherClientSparkUUID, otherClientSparkId, response.getJSONObject("offer"));
    }

    private void gotNewAnswer(CineMessage response) {
        String otherClientSparkId = response.getString("sparkId");
        String otherClientSparkUUID = response.getString("sparkUUID");
        mPeerConnectionsManager.newAnswer(otherClientSparkUUID, otherClientSparkId, response.getJSONObject("answer"));
    }

    private void userLeft(CineMessage response) {
        String otherClientSparkId = response.getString("sparkId");
        String otherClientSparkUUID = response.getString("sparkUUID");
        String room = response.getString("room");
        mPeerConnectionsManager.closeConnection(otherClientSparkUUID);
        try {
            JSONObject j = new JSONObject();
            j.put("action", "room-goodbye");
            j.put("room", room);
            sendToOtherSpark(otherClientSparkId, j);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    //    "{\"action\":\"member\",\"room\":\"hello\",\"sparkId\":\"5fa684d5-708b-4674-b548-b8b12011aa02\"}"
    private void userJoined(CineMessage response) {
        String otherClientSparkId = response.getString("sparkId");
        String otherClientSparkUUID = response.getString("sparkUUID");
        mPeerConnectionsManager.ensurePeerConnection(otherClientSparkUUID, otherClientSparkId, true);
        try {
            String room = response.getString("room");
            JSONObject j = new JSONObject();
            j.put("action", "room-announce");
            j.put("room", room);
            sendToOtherSpark(otherClientSparkId, j);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleCall(CineMessage response) {
        Call call = callFromFromRoom(response.getString("room"), false);
        config.getCinePeerRenderer().onCall(call);
    }

    private void handleCallCancel(CineMessage response) {
        Call call = callFromFromRoom(response.getString("room"), false);
        call.cancelled(response.getString("identity"));
    }

    private void handleCallReject(CineMessage response) {
        Call call = callFromFromRoom(response.getString("room"), false);
        call.rejected(response.getString("identity"));
    }

    private Call callFromFromRoom(String room, boolean initiated){
        Call call = calls.get(room);
        if (call == null){
            call = new Call(room, this, initiated);
            calls.put(room, call);
        }
        return call;
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
        //BASE
        if (action.equals("ack")) {
            Log.v(TAG, "GOT ACK");
            // do nothing
        } else if (action.equals("error")) {
            Log.v(TAG, "GOT ERRORS");
            config.getCinePeerRenderer().onError(message);
        } else if (action.equals("rtc-servers")) {
            Log.v(TAG, "GOT ALL SERVERS");
            gotAllServers(message);
        //END BASE
        //CALLING
        } else if (action.equals("call")) {
            Log.v(TAG, "GOT new incoming call");
            handleCall(message);
        } else if (action.equals("call-cancel")) {
            Log.v(TAG, "GOT new incoming call cancel");
            handleCallCancel(message);
        } else if (action.equals("call-reject")) {
            Log.v(TAG, "GOT new incoming call reject");
            handleCallReject(message);
        //END CALLING
        //ROOMS
        } else if (action.equals("room-join")) {
            Log.v(TAG, "GOT new member");
            userJoined(message);
        } else if (action.equals("room-leave")) {
            Log.v(TAG, "GOT new leave");
            userLeft(message);
        } else if (action.equals("room-announce")) {
            Log.v(TAG, "GOT room announce");
            roomAnnounce(message);
        } else if (action.equals("room-goodbye")) {
            Log.v(TAG, "GOT room goodbye");
            roomGoodbye(message);
        //END ROOMS
        //RTC
        } else if (action.equals("rtc-ice")) {
            Log.v(TAG, "GOT new ice");
            gotNewIce(message);
        } else if (action.equals("rtc-offer")) {
            Log.v(TAG, "GOT new offer");
            gotNewOffer(message);
        } else if (action.equals("rtc-answer")) {
            Log.v(TAG, "GOT new answer");
            gotNewAnswer(message);
        //END RTC
        } else {
            Log.v(TAG, "Unknown action");
        }
    }

    private void roomGoodbye(CineMessage message) {
        String otherClientSparkUUID = message.getString("sparkUUID");
        mPeerConnectionsManager.closeConnection(otherClientSparkUUID);
    }

    private void roomAnnounce(CineMessage message) {
        String otherClientSparkId = message.getString("sparkId");
        String otherClientSparkUUID = message.getString("sparkUUID");
        mPeerConnectionsManager.ensurePeerConnection(otherClientSparkUUID, otherClientSparkId, false);
    }


    public void setPeerConnectionsManager(PeerConnectionsManager mPeerConnectionsManager) {
        this.mPeerConnectionsManager = mPeerConnectionsManager;
    }

}
