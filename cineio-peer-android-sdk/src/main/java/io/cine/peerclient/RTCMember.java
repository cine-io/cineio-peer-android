package io.cine.peerclient;

import org.webrtc.PeerConnection;

/**
 * Created by thomas on 9/15/14.
 */
public class RTCMember {

    private String sparkUUID;
    private String sparkId;
    private PeerConnection peerConnection;
    private PeerObserver peerObserver;
    private SDPObserver sdpObserver;

    public RTCMember(String sparkUUID) {
        this.sparkUUID = sparkUUID;
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

    public SDPObserver getSdpObserver() {
        return sdpObserver;
    }

    public void setSdpObserver(SDPObserver sdpObserver) {
        this.sdpObserver = sdpObserver;
    }

    public void close() {
        PeerConnection pc = getPeerConnection();
        PeerObserver observer = getPeerObserver();
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
}
