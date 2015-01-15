package io.cine.peerclient;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

/**
 * Created by thomas on 9/12/14.
 */
// https://github.com/secretapphd/droidkit-webrtc/blob/master/webrtc-sample/src/main/java/com/droidkit/webrtc/sample/AppRTCDemoActivity.java
// Implementation detail: handle offer creation/signaling and answer setting,
// as well as adding remote ICE candidates once the answer SDP is set.
public class LocalAnswerSDPObserver implements SdpObserver {
    private static final String TAG = "LocalOfferSDPObserver";

    private final CinePeerClient mCinePeerClient;
    private final RTCMember mRTCMember;
    private SessionDescription localSDP;


    public LocalAnswerSDPObserver(RTCMember rtc, CinePeerClient cinePeerClient) {
        this.mRTCMember = rtc;
        this.mCinePeerClient = cinePeerClient;
    }

    @Override
    public void onCreateSuccess(final SessionDescription localSDP) {
//        RTCHelper.abortUnless(localSDP == null, "multiple SDP create?!?");
        final SessionDescription sdp = new SessionDescription(
                localSDP.type, RTCHelper.preferISAC(localSDP.description));
        this.localSDP = localSDP;
        final LocalAnswerSDPObserver self = this;
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
        Log.d(TAG, "Sending " + localSDP.type);
        mCinePeerClient.getSignalingConnection().sendLocalDescription(mRTCMember.getSparkId(), localSDP);
    }

    @Override
    public void onSetSuccess() {
        mCinePeerClient.runOnUiThread(new Runnable() {
            public void run() {
                sendLocalDescription();
            }
        });
    }

    @Override
    public void onCreateFailure(final String error) {
        Log.w(TAG, "COULD NOT CREATE DESCRIPTION: " + error);
//        mCinePeerClient.runOnUiThread(new Runnable() {
//            public void run() {
//                throw new RuntimeException("createSDP error: " + error);
//            }
//        });
    }

    @Override
    public void onSetFailure(final String error) {
        Log.w(TAG, "COULD NOT SET DESCRIPTION: " + error);

//        mCinePeerClient.runOnUiThread(new Runnable() {
//            public void run() {
//                throw new RuntimeException("setSDP error: " + error);
//            }
//        });
    }
}
