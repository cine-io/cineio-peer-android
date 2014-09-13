package io.cine.androidwebsocket;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
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
    private final SDPObserver self;
    private final StartRTC mStartRTC;
    private final String mOtherClientSparkId;
    private SessionDescription localSdp;
    private Toast logToast;
    private LinkedList<IceCandidate> queuedRemoteCandidates =
            new LinkedList<IceCandidate>();

        public SDPObserver(StartRTC startRTC, String otherClientSparkId, PeerConnection pc, Activity context) {
            mStartRTC = startRTC;
            mContext = context;
            peerConnection = pc;
            mOtherClientSparkId = otherClientSparkId;
            self = this;
        }

    @Override public void onCreateSuccess(final SessionDescription origSdp) {
            abortUnless(localSdp == null, "multiple SDP create?!?");
            final SessionDescription sdp = new SessionDescription(
                    origSdp.type, preferISAC(origSdp.description));
            localSdp = sdp;
            mContext.runOnUiThread(new Runnable() {
                public void run() {
                    peerConnection.setLocalDescription(self, sdp);
                }
            });
        }

    // Poor-man's assert(): die with |msg| unless |condition| is true.
    private static void abortUnless(boolean condition, String msg) {
        if (!condition) {
            throw new RuntimeException(msg);
        }
    }


    // Mangle SDP to prefer ISAC/16000 over any other audio codec.
    public static String preferISAC(String sdpDescription) {
        String[] lines = sdpDescription.split("\r\n");
        int mLineIndex = -1;
        String isac16kRtpMap = null;
        Pattern isac16kPattern =
                Pattern.compile("^a=rtpmap:(\\d+) ISAC/16000[\r]?$");
        for (int i = 0;
             (i < lines.length) && (mLineIndex == -1 || isac16kRtpMap == null);
             ++i) {
            if (lines[i].startsWith("m=audio ")) {
                mLineIndex = i;
                continue;
            }
            Matcher isac16kMatcher = isac16kPattern.matcher(lines[i]);
            if (isac16kMatcher.matches()) {
                isac16kRtpMap = isac16kMatcher.group(1);
                continue;
            }
        }
        if (mLineIndex == -1) {
            Log.d(TAG, "No m=audio line, so can't prefer iSAC");
            return sdpDescription;
        }
        if (isac16kRtpMap == null) {
            Log.d(TAG, "No ISAC/16000 line, so can't prefer iSAC");
            return sdpDescription;
        }
        String[] origMLineParts = lines[mLineIndex].split(" ");
        StringBuilder newMLine = new StringBuilder();
        int origPartIndex = 0;
        // Format is: m=<media> <port> <proto> <fmt> ...
        newMLine.append(origMLineParts[origPartIndex++]).append(" ");
        newMLine.append(origMLineParts[origPartIndex++]).append(" ");
        newMLine.append(origMLineParts[origPartIndex++]).append(" ");
        newMLine.append(isac16kRtpMap);
        for (; origPartIndex < origMLineParts.length; ++origPartIndex) {
            if (!origMLineParts[origPartIndex].equals(isac16kRtpMap)) {
                newMLine.append(" ").append(origMLineParts[origPartIndex]);
            }
        }
        lines[mLineIndex] = newMLine.toString();
        StringBuilder newSdpDescription = new StringBuilder();
        for (String line : lines) {
            newSdpDescription.append(line).append("\r\n");
        }
        return newSdpDescription.toString();
    }

    // Helper for sending local SDP (offer or answer, depending on role) to the
        // other participant.  Note that it is important to send the output of
        // create{Offer,Answer} and not merely the current value of
        // getLocalDescription() because the latter may include ICE candidates that
        // we might want to filter elsewhere.
        private void sendLocalDescription() {
            logAndToast("Sending " + localSdp.type);
            JSONObject sdpJSON = new JSONObject();
            String type = localSdp.type.canonicalForm();
            jsonPut(sdpJSON, "type", type);
            jsonPut(sdpJSON, "sdp", localSdp.description);
            JSONObject json = new JSONObject();
            jsonPut(json, type, sdpJSON);
            jsonPut(json, "sparkId", mOtherClientSparkId);
            jsonPut(json, "action", type);
            sendMessage(json);
        }

    // Send |json| to the underlying AppEngine Channel.
    private void sendMessage(JSONObject json) {
        mStartRTC.sendMessage(json);
    }

    // Put a |key|->|value| mapping in |json|.
    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    @Override public void onSetSuccess() {
            mContext.runOnUiThread(new Runnable() {
                public void run() {
                    if (mStartRTC.isInitiator()) {
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
                            logAndToast("Creating answer");
                            peerConnection.createAnswer(SDPObserver.this, mStartRTC.getConstraints());
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
