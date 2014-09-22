package io.cine.peerclient;

import org.webrtc.PeerConnection;

/**
 * Created by thomas on 9/15/14.
 */
public class RTCMember {

    private final String sparkId;
    private PeerConnection peerConnection;
    private PeerObserver peerObserver;
    private SDPObserver sdpObserver;

    public RTCMember(String sparkId) {
        this.sparkId = sparkId;
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
}
