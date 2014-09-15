package io.cine.androidwebsocket;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by thomas on 9/12/14.
 */
// https://github.com/secretapphd/droidkit-webrtc/blob/master/webrtc-sample/src/main/java/com/droidkit/webrtc/sample/AppRTCDemoActivity.java
// Implementation detail: handle offer creation/signaling and answer setting,
// as well as adding remote ICE candidates once the answer SDP is set.
public class SDPObserver implements SdpObserver {
    private static final String TAG = "SDPObserver";

    private final Activity mContext;
    private final PeerConnection peerConnection;
    private final String mOtherClientSparkId;
    private final Primus primus;
    private final MediaConstraints constraints;
    private final boolean isInitiator;
    private SessionDescription localSdp;
    private Toast logToast;
    private LinkedList<IceCandidate> queuedRemoteCandidates =
            new LinkedList<IceCandidate>();

    public SDPObserver(String otherClientSparkId, PeerConnection peerConnection, MediaConstraints constraints, Primus primus, MyActivity mActivity, boolean isInitiator) {
        mContext = mActivity;
        this.peerConnection = peerConnection;
        mOtherClientSparkId = otherClientSparkId;
        this.primus = primus;
        this.constraints = constraints;
        this.isInitiator = isInitiator;
    }

    @Override public void onCreateSuccess(final SessionDescription origSdp) {
            RTCHelper.abortUnless(localSdp == null, "multiple SDP create?!?");
            final SessionDescription sdp = new SessionDescription(
                    origSdp.type, RTCHelper.preferISAC(origSdp.description));
            localSdp = sdp;
        final SDPObserver self = this;
        mContext.runOnUiThread(new Runnable() {
                public void run() {
                    peerConnection.setLocalDescription(self, sdp);
                }
            });
        }

    // Helper for sending local SDP (offer or answer, depending on role) to the
        // other participant.  Note that it is important to send the output of
        // create{Offer,Answer} and not merely the current value of
        // getLocalDescription() because the latter may include ICE candidates that
        // we might want to filter elsewhere.
        private void sendLocalDescription() {
            Log.d(TAG, "Sending " + localSdp.type);
            JSONObject sdpJSON = new JSONObject();
            String type = localSdp.type.canonicalForm();
            jsonPut(sdpJSON, "type", type);
            jsonPut(sdpJSON, "sdp", localSdp.description);
            JSONObject json = new JSONObject();
            jsonPut(json, type, sdpJSON);
            jsonPut(json, "action", type);
            primus.sendToOtherSpark(mOtherClientSparkId, json);
        }

    // Put a |key|->|value| mapping in |json|.
    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void onSetSuccess() {
            mContext.runOnUiThread(new Runnable() {
                public void run() {
                    if (isInitiator) {
                        if (peerConnection.getRemoteDescription() != null) {
                            // We've set our local offer and received & set the remote
                            // answer, so drain candidates.
                            drainRemoteCandidates();
                        } else {
                            // We've just set our local description so time to send it.
                            sendLocalDescription();
                        }
                    } else {
                        if (peerConnection.getLocalDescription() == null) {
                            // We just set the remote offer, time to create our answer.
                            Log.d(TAG, "Creating answer");
                            peerConnection.createAnswer(SDPObserver.this, constraints);
                        } else {
                            // Answer now set as local description; send it and drain
                            // candidates.
                            sendLocalDescription();
                            drainRemoteCandidates();
                        }
                    }
                }
            });
        }

        @Override public void onCreateFailure(final String error) {
            mContext.runOnUiThread(new Runnable() {
                public void run() {
                    throw new RuntimeException("createSDP error: " + error);
                }
            });
        }
    @Override public void onSetFailure(final String error) {
        mContext.runOnUiThread(new Runnable() {
            public void run() {
                throw new RuntimeException("setSDP error: " + error);
            }
        });
    }

    private void drainRemoteCandidates() {
        for (IceCandidate candidate : queuedRemoteCandidates) {
            peerConnection.addIceCandidate(candidate);
        }
        queuedRemoteCandidates = null;
    }
}
