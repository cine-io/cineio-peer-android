package io.cine.peerclient;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.PeerConnection;

/**
 * Created by thomas on 9/15/14.
 */
public class RTCMember {

    private static final String TAG = "RTCMember";
    private final JSONObject support;
    private String sparkUUID;
    private String sparkId;
    private PeerConnection peerConnection;
    private PeerObserver peerObserver;
    private DataChannel mainDataChannel;
    private final SignalingConnection signalingConnection;
    private boolean iceGatheringComplete;
    private boolean waitingToSendLocalDescription;

    public RTCMember(String sparkUUID, SignalingConnection signalingConnection, JSONObject support) {
        Log.v(TAG, "Creating rtc member");
        this.sparkUUID = sparkUUID;
        this.signalingConnection = signalingConnection;
        this.support = support;
    }

    public PeerConnection getPeerConnection() {
        return peerConnection;
    }

    public void setPeerConnection(PeerConnection peerConnection) {
        this.peerConnection = peerConnection;
    }

    public PeerObserver getPeerObserver() {
        return peerObserver;
    }

    public void setPeerObserver(PeerObserver peerObserver) {
        this.peerObserver = peerObserver;
    }


    public void close() {
        PeerConnection pc = getPeerConnection();
        PeerObserver observer = getPeerObserver();
        DataChannel channel = getMainDataChannel();
        if (channel != null) {
            channel.close();
        }
        if (observer != null) {
            observer.dispose();
        }
        if (pc != null) {
            pc.close();
            // causes the app to crash
            // pc.dispose();
        }
    }

    public String getSparkId() {
        return sparkId;
    }

    public void setClientSparkId(String clientSparkId) {
        this.sparkId = clientSparkId;
    }

    public void setMainDataChannel(final DataChannel mainDataChannel) {
        Log.v(TAG, "SETTING DATA CHANNEL");
        this.mainDataChannel = mainDataChannel;
        this.mainDataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onStateChange() {
                Log.v(TAG, "Got new state");
                Log.v(TAG, "NEW STATE: " + mainDataChannel.state().name());
                //TODO, send pending messages
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                Log.v(TAG, "Got new message");
            }
        });
    }

    public DataChannel getMainDataChannel() {
        return mainDataChannel;
    }

    public void markIceComplete() {
        this.iceGatheringComplete = true;
        if (this.waitingToSendLocalDescription){
            Log.v(TAG, "markIceComplete waitingToSendLocalDescription");
            sendLocalDescription();
        }
    }

    public void localDescriptionReady() {
        if (supportsTrickleIce()){
            Log.v(TAG, "localDescriptionReady supportsTrickleIce");
            sendLocalDescription();
        } else if (iceGatheringComplete){
            Log.v(TAG, "localDescriptionReady iceGatheringComplete");
            sendLocalDescription();
        } else {
            Log.v(TAG, "localDescriptionReady waitingToSendLocalDescription");
            this.waitingToSendLocalDescription = true;
        }
    }

    private void sendLocalDescription() {
        signalingConnection.sendLocalDescription(getSparkId(), this.peerConnection.getLocalDescription());
    }

    private boolean supportsTrickleIce() {
        try {
            return support.getBoolean("trickleIce");
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
}
