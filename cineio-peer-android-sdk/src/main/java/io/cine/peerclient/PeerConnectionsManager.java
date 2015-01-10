package io.cine.peerclient;

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
    private final CinePeerClient mCinePeerClient;
    private final HashMap<String, RTCMember> rtcMembers;
    private MediaStream localMediaStream;
    private ArrayList<PeerConnection.IceServer> servers;

    public PeerConnectionsManager(CinePeerClient cinePeerClient) {
        mCinePeerClient = cinePeerClient;
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

    public void ensurePeerConnection(String otherClientUUID, String otherClientSparkId, boolean isInitiator) {
        getPeerConnection(otherClientUUID, otherClientSparkId, isInitiator);
    }

    //    TODO: ensure iceServers are added
    private PeerConnection createPeerConnection(String otherClientSparkUUID, String otherClientSparkId, boolean isInitiator) {
        Log.d(TAG, "creating new peer connection for: " + otherClientSparkUUID);
        RTCMember rtc = new RTCMember(otherClientSparkUUID);
        rtc.setClientSparkId(otherClientSparkId);
        PeerObserver observer = new PeerObserver(rtc, mCinePeerClient);
        rtc.setPeerObserver(observer);

        Log.d(TAG, "created new peer observer");

        PeerConnection peerConnection = factory.createPeerConnection(servers, mCinePeerClient.getMediaConstraints(), observer);
        rtc.setPeerConnection(peerConnection);
        Log.d(TAG, "created new peerConnection");
//        this is supposed to be a blank media constraints
        peerConnection.addStream(localMediaStream, new MediaConstraints());
        Log.d(TAG, "added stream");

        SDPObserver sdpObserver = new SDPObserver(rtc, mCinePeerClient, isInitiator);
        Log.d(TAG, "created sdpObserver");
        rtc.setSdpObserver(sdpObserver);

        if (isInitiator) {
            Log.d(TAG, "creating offer");
            peerConnection.createOffer(sdpObserver, mCinePeerClient.getMediaConstraints());
        }
        rtcMembers.put(otherClientSparkUUID, rtc);
        return peerConnection;
    }

    public void newIce(String otherClientSparkUUID, String otherClientSparkId, JSONObject candidateObj) {
        try {
            PeerConnection pc = getPeerConnection(otherClientSparkUUID, otherClientSparkId, false);
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

    private PeerConnection getPeerConnection(String otherClientSparkUUID, String otherClientSparkId, boolean isInitiator) {
        RTCMember rtc = rtcMembers.get(otherClientSparkUUID);

        if (rtc != null) {
            rtc.setClientSparkId(otherClientSparkId);
            return rtc.getPeerConnection();
        } else {
            return createPeerConnection(otherClientSparkUUID, otherClientSparkId, isInitiator);
        }
    }

    public void newOffer(String otherClientSparkUUID, String otherClientSparkId, JSONObject offerObj) {
        PeerConnection pc = getPeerConnection(otherClientSparkUUID, otherClientSparkId, false);
        try {
            String type = offerObj.getString("type");
            String sdpDescription = offerObj.getString("sdp");
            SessionDescription sd = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(type),
                    RTCHelper.preferISAC(sdpDescription));
            SDPObserver sdpObserver = getSDPObserverFromSparkUUID(otherClientSparkUUID);
            pc.setRemoteDescription(sdpObserver, sd);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public SDPObserver getSDPObserverFromSparkUUID(String otherClientSparkUUID) {
        return rtcMembers.get(otherClientSparkUUID).getSdpObserver();
    }

    public void setLocalMediaStream(MediaStream localMediaStream) {
        this.localMediaStream = localMediaStream;
    }

    // notice how close this is to newOffer
    // an offer is the remote description as is an answer
    public void newAnswer(String otherClientSparkUUID, String otherClientSparkId, JSONObject answerObj) {
        PeerConnection pc = getPeerConnection(otherClientSparkUUID, otherClientSparkId, false);
        try {
            String type = answerObj.getString("type");
            String sdpDescription = answerObj.getString("sdp");
            SessionDescription sd = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(type),
                    RTCHelper.preferISAC(sdpDescription));
            SDPObserver sdpObserver = getSDPObserverFromSparkUUID(otherClientSparkUUID);
            pc.setRemoteDescription(sdpObserver, sd);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void closeConnection(String otherClientSparkUUID) {
        RTCMember rtc = rtcMembers.remove(otherClientSparkUUID);
        if (rtc != null) {
            rtc.close();
        }
    }

    public void end() {
        for (String sparkUUID : rtcMembers.keySet()) {
            closeConnection(sparkUUID);
        }
    }
}
