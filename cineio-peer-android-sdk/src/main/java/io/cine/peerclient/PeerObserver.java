package io.cine.peerclient;

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
    private final RTCMember rtcMember;
    private final CinePeerClient mCinePeerClient;
    private MediaStream addedStream;

    public PeerObserver(RTCMember rtc, CinePeerClient cinePeerClient) {
        this.rtcMember = rtc;
        this.mCinePeerClient = cinePeerClient;

    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        Log.d(TAG, "onIceCandidate");
        mCinePeerClient.getSignalingConnection().sendIceCandidate(rtcMember.getSparkId(), candidate);
    }

    @Override
    public void onAddStream(final MediaStream stream) {
        Log.d(TAG, "onAddStream");
        addedStream = stream;
        RTCHelper.abortUnless(stream.audioTracks.size() <= 1 && stream.videoTracks.size() <= 1, "Weird-looking stream: " + stream);
        if (stream.videoTracks.size() == 1) {
            mCinePeerClient.addStream(stream);
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
    public void onDataChannel(DataChannel dataChannel) {
        Log.d(TAG, "onDataChannel");
        this.rtcMember.setMainDataChannel(dataChannel);
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
        mCinePeerClient.removeStream(stream);
    }
}
