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
public class RemoteOfferSDPObserver implements SdpObserver {
    private static final String TAG = "RemoteAnswerSDPObserver";

    private final CinePeerClient mCinePeerClient;
    private final RTCMember mRTCMember;

    public RemoteOfferSDPObserver(RTCMember rtc, CinePeerClient cinePeerClient) {
        this.mRTCMember = rtc;
        this.mCinePeerClient = cinePeerClient;
    }

    @Override
    public void onCreateSuccess(final SessionDescription origSdp) {
        Log.e(TAG, "SHOULD NOT HAVE CREATED A REMOTE OFFER");
    }

    @Override
    public void onSetSuccess() {
        mCinePeerClient.runOnUiThread(new Runnable() {
            public void run() {
                Log.v(TAG, "SET REMOTE OFFER");
                PeerConnection peerConnection = mRTCMember.getPeerConnection();
                LocalAnswerSDPObserver localAnswerSDPObserver = new LocalAnswerSDPObserver(mRTCMember, mCinePeerClient);
                peerConnection.createAnswer(localAnswerSDPObserver, mCinePeerClient.getMediaConstraints());
            }
        });
    }

    @Override
    public void onCreateFailure(final String error) {
        Log.w(TAG, "COULD NOT CREATE DESCRIPTION: " + error);
    }

    @Override
    public void onSetFailure(final String error) {
        Log.w(TAG, "COULD NOT SET DESCRIPTION: " + error);
    }
}
