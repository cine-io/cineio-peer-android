package io.cine.androidwebsocket;

import android.app.Activity;
import android.util.Log;

import com.koushikdutta.async.http.WebSocket;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioTrack;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;

import java.util.ArrayList;

/**
 * Created by thomas on 9/11/14.
 */
public class StartRTC {
    private static final String TAG = "StartRTC";
    private SDPObserver sdpObserver;
    private final MyActivity mActivity;

    private ArrayList<PeerConnection.IceServer> servers;
    private static PeerConnectionFactory factory;
    private boolean mIsInitiator;
    private PeerConnection peerConnection;

    public MediaConstraints getConstraints() {
        return constraints;
    }

    public static PeerConnectionFactory getFactory(){
        if (factory != null){
            return factory;
        }
        factory = new PeerConnectionFactory();
        return factory;
    }

    private MediaConstraints constraints;
    private WebSocket mWebSocket;
    private MediaStream mediaStream;

    public StartRTC(MyActivity activity){
        mActivity = activity;
        servers = new ArrayList<PeerConnection.IceServer>();
    }
    public void setSignalingConnection(WebSocket ws){
        this.mWebSocket = ws;
    }

    public void addIceServer(String iceServer){
        servers.add(new PeerConnection.IceServer(iceServer));
    }

    public void newMember(String otherClientSparkId) {
        getPeerConnection(otherClientSparkId, true);
    }

    public PeerConnection createPeerConnection(String otherClientSparkId, boolean isInitiator){
        mIsInitiator = isInitiator;
        final PeerObserver observer = new PeerObserver(mActivity, this, otherClientSparkId);

        peerConnection = factory.createPeerConnection(servers, constraints, observer);
        peerConnection.addStream(mediaStream, new MediaConstraints());

//        peerConnection.createOffer(new SdpObserver() {
//            @Override
//            public void onCreateSuccess(SessionDescription sdp) {
//                Log.d(TAG, "onCreateSuccess");
//            }
//
//            @Override
//            public void onSetSuccess() {
//                Log.d(TAG, "onSetSuccess");
//            }
//
//            @Override
//            public void onCreateFailure(String error) {
//                Log.d(TAG, "onCreateFailure");
//            }
//
//            @Override
//            public void onSetFailure(String error) {
//                Log.d(TAG, "onSetFailure");
//            }
//        }, constraints);

        sdpObserver = new SDPObserver(this, otherClientSparkId, peerConnection, mActivity);
        if (isInitiator) {
            peerConnection.createOffer(sdpObserver, constraints);
        }
        return peerConnection;
    }

    public void start() {
        constraints = new MediaConstraints();

        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));

        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
    }

    public void sendMessage(JSONObject j) {
        try {
            j.put("source", "android");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "SENDING STUFF: "+j.toString());

        mWebSocket.send(j.toString());
    }

    public boolean isInitiator() {
        return mIsInitiator;
    }

    public void newIce(String otherClientSparkId, JSONObject candidateObj) {
        try {
            PeerConnection pc = getPeerConnection(otherClientSparkId, false);
            JSONObject j = candidateObj.getJSONObject("candidate");
            int sdpMLineIndex = j.getInt("sdpMLineIndex");
            String sdpMid = j.getString("sdpMid");
            String candidate = j.getString("candidate");
            Log.v(TAG, "sdpMLineIndex: "+sdpMLineIndex);
            Log.v(TAG, "sdpMid: "+sdpMid);
            Log.v(TAG, "candidate: "+candidate);
            IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, candidate);
            pc.addIceCandidate(iceCandidate);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    //            TOOD: MAKE THIS USE A HASHMAP
    private PeerConnection getPeerConnection(String otherClientSparkId, boolean isInitiator) {
        if (peerConnection != null){
            return peerConnection;
        }else{
            createPeerConnection(otherClientSparkId, isInitiator);
            return peerConnection;
        }
    }

    //    TODO: return answer
    public void newOffer(String otherClientSparkId, JSONObject offerObj) {
        PeerConnection pc = getPeerConnection(otherClientSparkId, false);
        try {
            String type = offerObj.getString("type");
            String sdpDescription = offerObj.getString("sdp");
            SessionDescription sd = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(type),
                    SDPObserver.preferISAC(sdpDescription));
            pc.setRemoteDescription(sdpObserver, sd);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void setMediaStream(MediaStream mediaStream) {
        this.mediaStream = mediaStream;
    }
}
