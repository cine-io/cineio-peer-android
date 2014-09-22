package io.cine.peerclient;

import android.util.Log;

import org.webrtc.PeerConnection;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

/**
 * Created by thomas on 9/12/14.
 */
// https://github.com/secretapphd/droidkit-webrtc/blob/master/webrtc-sample/src/main/java/com/droidkit/webrtc/sample/AppRTCDemoActivity.java
// Implementation detail: handle offer creation/signaling and answer setting,
// as well as adding remote ICE candidates once the answer SDP is set.
public class SDPObserver implements SdpObserver {
    private static final String TAG = "SDPObserver";

    private final CinePeerClient mCinePeerClient;
    private final boolean isInitiator;
    private final RTCMember mRTCMember;
    private SessionDescription localSdp;


    public SDPObserver(RTCMember rtc, CinePeerClient cinePeerClient, boolean isInitiator) {
        this.mRTCMember = rtc;
        this.mCinePeerClient = cinePeerClient;
        this.isInitiator = isInitiator;

    }

    @Override
    public void onCreateSuccess(final SessionDescription origSdp) {
        RTCHelper.abortUnless(localSdp == null, "multiple SDP create?!?");
        final SessionDescription sdp = new SessionDescription(
                origSdp.type, RTCHelper.preferISAC(origSdp.description));
        localSdp = sdp;
        final SDPObserver self = this;
        mCinePeerClient.runOnUiThread(new Runnable() {
            public void run() {
                mRTCMember.getPeerConnection().setLocalDescription(self, sdp);
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
        mCinePeerClient.getSignalingConnection().sendLocalDescription(mRTCMember.getSparkId(), localSdp);
    }

    @Override
    public void onSetSuccess() {
        mCinePeerClient.runOnUiThread(new Runnable() {
            public void run() {
                PeerConnection peerConnection = mRTCMember.getPeerConnection();
                if (isInitiator) {
                    if (peerConnection.getRemoteDescription() != null) {
                        // We've set our local offer and received & set the remote
                        // answer, so drain candidates.
                        // we don't need to drain candidates, they've already been added
                    } else {
                        // We've just set our local description so time to send it.
                        sendLocalDescription();
                    }
                } else {
                    if (peerConnection.getLocalDescription() == null) {
                        // We just set the remote offer, time to create our answer.
                        Log.d(TAG, "Creating answer");
                        peerConnection.createAnswer(SDPObserver.this, mCinePeerClient.getMediaConstraints());
                    } else {
                        // Answer now set as local description; send it and drain
                        // candidates.
                        sendLocalDescription();
                    }
                }
            }
        });
    }

    @Override
    public void onCreateFailure(final String error) {
//        mCinePeerClient.runOnUiThread(new Runnable() {
//            public void run() {
//                throw new RuntimeException("createSDP error: " + error);
//            }
//        });
    }

    @Override
    public void onSetFailure(final String error) {
//        mCinePeerClient.runOnUiThread(new Runnable() {
//            public void run() {
//                throw new RuntimeException("setSDP error: " + error);
//            }
//        });
    }
}
