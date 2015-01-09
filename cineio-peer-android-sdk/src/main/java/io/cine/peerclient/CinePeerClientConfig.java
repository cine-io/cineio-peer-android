package io.cine.peerclient;

import android.app.Activity;
import android.util.Log;

import org.webrtc.MediaConstraints;

/**
 * Created by thomas on 9/22/14.
 */
public class CinePeerClientConfig {
    private static final String TAG = "CinePeerClientConfig";

    private final Activity mActivity;
    private final String mPublicKey;
    private final CinePeerRenderer mCinePeerRenderer;
    private boolean hasVideo;
    private boolean hasAudio;

    public CinePeerClientConfig(String publicKey, Activity activity) {
        mActivity = activity;
        mPublicKey = publicKey;
        if (mActivity instanceof CinePeerRenderer) {
            this.mCinePeerRenderer = (CinePeerRenderer) mActivity;
        } else {
            throw new RuntimeException("Activity does not implement CinePeerRenderer please use constructor (Activity, CinePeerRenderer)");
        }
    }


    public boolean hasVideo() {
        return hasVideo;
    }

    public void setVideo(boolean hasVideo) {
        this.hasVideo = hasVideo;
    }

    public boolean hasAudio() {
        return hasAudio;
    }

    public void setAudio(boolean hasAudio) {
        this.hasAudio = hasAudio;
    }

    public Activity getActivity() {
        return mActivity;
    }

    public String getApiKey() {
        return mPublicKey;
    }

    public MediaConstraints getMediaConstraints() {
        MediaConstraints constraints = new MediaConstraints();

        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));

        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        Log.d(TAG, "created new constraints");
        return constraints;
    }

    public CinePeerRenderer getCinePeerRenderer() {
        return mCinePeerRenderer;
    }
}
