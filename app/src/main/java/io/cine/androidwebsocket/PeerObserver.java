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
    private final SignalingConnection signalingConnection;
    private final String mOtherClientSparkId;
    private final MyActivity mActivity;
    private MediaStream addedStream;

    public PeerObserver(MyActivity activity, SignalingConnection signalingConnection, String otherClientSparkId) {
        mActivity = activity;
        this.signalingConnection = signalingConnection;
        mOtherClientSparkId = otherClientSparkId;
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        Log.d(TAG, "onIceCandidate");
        signalingConnection.sendIceCandidate(mOtherClientSparkId, candidate);
    }

    @Override
    public void onAddStream(final MediaStream stream) {
        Log.d(TAG, "onAddStream");
        addedStream = stream;
        RTCHelper.abortUnless(stream.audioTracks.size() <= 1 && stream.videoTracks.size() <= 1, "Weird-looking stream: " + stream);
        if (stream.videoTracks.size() == 1) {
            mActivity.addStream(stream);
        }
    }

    @Override
    public void onRemoveStream(MediaStream stream) {
        disposeOfStream(stream);
    }

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
    public void onError() {
        Log.d(TAG, "onError");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.d(TAG, "onDataChannel");
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded");
    }

    public void dispose() {
        disposeOfStream(addedStream);
    }

    private void disposeOfStream(final MediaStream stream) {
        Log.d(TAG, "removing stream");
        mActivity.removeStream(stream);
    }
}
