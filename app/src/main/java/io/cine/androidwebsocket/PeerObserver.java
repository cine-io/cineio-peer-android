package io.cine.androidwebsocket;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

/**
 * Created by thomas on 9/11/14.
 */
class PeerObserver implements PeerConnection.Observer {
    private static final String TAG = "PeerObserver";
    private final Primus primus;
    private final String mOtherClientSparkId;
    private final MyActivity mContext;

    public PeerObserver(MyActivity context, Primus primus, String otherClientSparkId) {
        mContext = context;
        this.primus = primus;
        mOtherClientSparkId = otherClientSparkId;
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {

        Log.d(TAG, "onIceCandidate");
        try {
            JSONObject candidateObject = new JSONObject();
            candidateObject.put("candidate", candidate.sdp);
            candidateObject.put("sdpMid", candidate.sdpMid);
            candidateObject.put("sdpMLineIndex", candidate.sdpMLineIndex);
            JSONObject j = new JSONObject();
            j.put("action", "ice");
            JSONObject candidateMiddleMan = new JSONObject();
            candidateMiddleMan.put("candidate", candidateObject);
            j.put("candidate", candidateMiddleMan);

            primus.sendToOtherSpark(mOtherClientSparkId, j);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAddStream(final MediaStream stream) {

        Log.d(TAG, "onAddStream");
        mContext.runOnUiThread(new Runnable() {
            public void run() {
                RTCHelper.abortUnless(stream.audioTracks.size() <= 1 && stream.videoTracks.size() <= 1, "Weird-looking stream: " + stream);
                if (stream.videoTracks.size() == 1) {
                    mContext.addStream(stream);
                }
            }
        });
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
