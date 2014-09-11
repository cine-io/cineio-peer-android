package io.cine.androidwebsocket;

import android.util.Log;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

/**
 * Created by thomas on 9/11/14.
 */
class PeerObserver implements PeerConnection.Observer {
    private static final String TAG = "PeerObserver";

    @Override
    public void onSignalingChange(PeerConnection.SignalingState newState) {
        Log.d(TAG, "onSignalingChange");
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
        Log.d(TAG, "onIceConnectionChange");
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
        Log.d(TAG, "onIceGatheringChange");
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        Log.d(TAG, "onIceCandidate");
    }

    @Override
    public void onError() {
        Log.d(TAG, "onError");
    }

    @Override
    public void onAddStream(MediaStream stream) {
        Log.d(TAG, "onAddStream");
    }

    @Override
    public void onRemoveStream(MediaStream stream) {
        Log.d(TAG, "onRemoveStream");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.d(TAG, "onDataChannel");
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded");
    }
}
