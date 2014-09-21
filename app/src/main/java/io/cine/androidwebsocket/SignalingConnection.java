package io.cine.androidwebsocket;

import android.app.Activity;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

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

    private SignalingConnection(Activity activity) {
        this.activity = activity;
        primus = Primus.connect(activity, baseUrl);
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
}
