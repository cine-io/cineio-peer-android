package io.cine.androidwebsocket;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
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
    private PeerConnection peerConnection;

    public static PeerConnectionFactory getFactory(){
        if (factory != null){
            return factory;
        }
        factory = new PeerConnectionFactory();
        return factory;
    }

    private Primus primus;
    private MediaStream mediaStream;

    public StartRTC(MyActivity activity, Primus primus){
        mActivity = activity;
        this.primus = primus;
        servers = new ArrayList<PeerConnection.IceServer>();
    }

    public void addStunServer(String iceServer){
        servers.add(new PeerConnection.IceServer(iceServer));
    }

    public void addTurnServer(String url, String username, String password) {
        servers.add(new PeerConnection.IceServer(url, username, password));
    }


    public void newMember(String otherClientSparkId) {
        getPeerConnection(otherClientSparkId, true);
    }

    //    TODO: ensure iceServers are added
    public PeerConnection createPeerConnection(String otherClientSparkId, boolean isInitiator){
        final PeerObserver observer = new PeerObserver(mActivity, primus, otherClientSparkId);

        MediaConstraints constraints = new MediaConstraints();

        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));

        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

        peerConnection = factory.createPeerConnection(servers, constraints, observer);
        peerConnection.addStream(mediaStream, new MediaConstraints());

        sdpObserver = new SDPObserver(otherClientSparkId, peerConnection, constraints, primus, mActivity, isInitiator);
        if (isInitiator) {
            peerConnection.createOffer(sdpObserver, constraints);
        }
        return peerConnection;
    }

    public void sendMessage(JSONObject j) {
        primus.send(j);
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

    public void newOffer(String otherClientSparkId, JSONObject offerObj) {
        PeerConnection pc = getPeerConnection(otherClientSparkId, false);
        try {
            String type = offerObj.getString("type");
            String sdpDescription = offerObj.getString("sdp");
            SessionDescription sd = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(type),
                    RTCHelper.preferISAC(sdpDescription));
            pc.setRemoteDescription(sdpObserver, sd);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void setMediaStream(MediaStream mediaStream) {
        this.mediaStream = mediaStream;
    }

    // notice how close this is to newOffer
    // an offer is the remote description as is an answer
    public void newAnswer(String otherClientSparkId, JSONObject answerObj) {
        PeerConnection pc = getPeerConnection(otherClientSparkId, false);
        try {
            String type = answerObj.getString("type");
            String sdpDescription = answerObj.getString("sdp");
            SessionDescription sd = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(type),
                    RTCHelper.preferISAC(sdpDescription));
            pc.setRemoteDescription(sdpObserver, sd);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}
