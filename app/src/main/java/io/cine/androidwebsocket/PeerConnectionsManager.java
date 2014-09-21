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
import java.util.HashMap;

/**
 * Created by thomas on 9/11/14.
 */
public class PeerConnectionsManager {
    private static final String TAG = "StartRTC";
    private static PeerConnectionFactory factory;
    private final MyActivity mActivity;
    private final HashMap<String, RTCMember> rtcMembers;
    private SignalingConnection signalingConnection;
    private MediaStream mediaStream;
    private ArrayList<PeerConnection.IceServer> servers;

    public PeerConnectionsManager(MyActivity activity, SignalingConnection signalingConnection) {
        mActivity = activity;
        this.signalingConnection = signalingConnection;
        this.rtcMembers = new HashMap<String, RTCMember>();
        servers = new ArrayList<PeerConnection.IceServer>();
    }

    public static PeerConnectionFactory getFactory() {
        if (factory != null) {
            return factory;
        }
        factory = new PeerConnectionFactory();
        return factory;
    }

    public void addStunServer(String url) {
        servers.add(new PeerConnection.IceServer(url));
    }

    public void addTurnServer(String url, String username, String password) {
        servers.add(new PeerConnection.IceServer(url, username, password));
    }


    public void newMember(String otherClientSparkId) {
        getPeerConnection(otherClientSparkId, true);
    }

    //    TODO: ensure iceServers are added
    private PeerConnection createPeerConnection(String otherClientSparkId, boolean isInitiator) {
        Log.d(TAG, "creating new peer connection for: " + otherClientSparkId);
        PeerObserver observer = new PeerObserver(mActivity, signalingConnection, otherClientSparkId);
        Log.d(TAG, "created new peer observer");

        MediaConstraints constraints = new MediaConstraints();

        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));

        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        Log.d(TAG, "created new constraints");

        PeerConnection peerConnection = factory.createPeerConnection(servers, constraints, observer);
        Log.d(TAG, "created new peerConnection");
        peerConnection.addStream(mediaStream, new MediaConstraints());
        Log.d(TAG, "added stream");

        SDPObserver sdpObserver = new SDPObserver(otherClientSparkId, peerConnection, constraints, signalingConnection, mActivity, isInitiator);
        Log.d(TAG, "created sdpObserver");

        if (isInitiator) {
            Log.d(TAG, "creating offer");
            peerConnection.createOffer(sdpObserver, constraints);
        }
        RTCMember rtc = new RTCMember(otherClientSparkId);
        rtc.setSdpObserver(sdpObserver);
        rtc.setPeerConnection(peerConnection);
        rtc.setPeerObserver(observer);
        rtcMembers.put(otherClientSparkId, rtc);
        return peerConnection;
    }

    public void newIce(String otherClientSparkId, JSONObject candidateObj) {
        try {
            PeerConnection pc = getPeerConnection(otherClientSparkId, false);
            JSONObject j = candidateObj.getJSONObject("candidate");
            int sdpMLineIndex = j.getInt("sdpMLineIndex");
            String sdpMid = j.getString("sdpMid");
            String candidate = j.getString("candidate");
            Log.v(TAG, "sdpMLineIndex: " + sdpMLineIndex);
            Log.v(TAG, "sdpMid: " + sdpMid);
            Log.v(TAG, "candidate: " + candidate);
            IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, candidate);
            pc.addIceCandidate(iceCandidate);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private PeerConnection getPeerConnection(String otherClientSparkId, boolean isInitiator) {
        RTCMember rtc = rtcMembers.get(otherClientSparkId);
        if (rtc != null) {
            return rtc.getPeerConnection();
        } else {
            return createPeerConnection(otherClientSparkId, isInitiator);
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
            SDPObserver sdpObserver = getSDPObserverFromSparkId(otherClientSparkId);
            pc.setRemoteDescription(sdpObserver, sd);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public SDPObserver getSDPObserverFromSparkId(String otherClientSparkId) {
        return rtcMembers.get(otherClientSparkId).getSdpObserver();
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
            SDPObserver sdpObserver = getSDPObserverFromSparkId(otherClientSparkId);
            pc.setRemoteDescription(sdpObserver, sd);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void memberLeft(String otherClientSparkId) {
        RTCMember rtc = rtcMembers.remove(otherClientSparkId);
        if (rtc != null) {
            rtc.dispose();
        }
    }
}
