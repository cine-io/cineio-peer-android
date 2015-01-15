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
public class RemoteAnswerSDPObserver implements SdpObserver {
    private static final String TAG = "RemoteAnswerSDPObserver";

    private final CinePeerClient mCinePeerClient;
    private final RTCMember mRTCMember;
    private SessionDescription remoteSDP;


    public RemoteAnswerSDPObserver(RTCMember rtc, CinePeerClient cinePeerClient) {
        this.mRTCMember = rtc;
        this.mCinePeerClient = cinePeerClient;
    }

    @Override
    public void onCreateSuccess(final SessionDescription origSdp) {
        Log.e(TAG, "SHOULD NOT HAVE CREATED A REMOTE ANSWER");
    }


    @Override
    public void onSetSuccess() {
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
