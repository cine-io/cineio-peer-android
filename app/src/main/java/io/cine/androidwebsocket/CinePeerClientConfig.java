package io.cine.androidwebsocket;

import android.app.Activity;
import android.util.Log;

import org.webrtc.MediaConstraints;

/**
 * Created by thomas on 9/22/14.
 */
public class CinePeerClientConfig {
    private static final String TAG = "CinePeerClientConfig";

    private final Activity mActivity;
    private final String mApiKey;
    private final CinePeerRenderer mCinePeerRenderer;
    private boolean hasVideo;
    private boolean hasAudio;

    public CinePeerClientConfig(String apiKey, Activity activity){
        mActivity = activity;
        mApiKey = apiKey;
        if (mActivity instanceof CinePeerRenderer){
            this.mCinePeerRenderer = (CinePeerRenderer) mActivity;
        } else{
            throw new RuntimeException("Activity does not implement CinePeerRenderer please use constructor (Activity, CinePeerRenderer)");
        }
    }

    public CinePeerClientConfig(String apiKey, Activity activity, CinePeerRenderer cinePeerRenderer){
        mActivity = activity;
        mApiKey = apiKey;
        mCinePeerRenderer = cinePeerRenderer;
    }


    public boolean getVideo() {
        return hasVideo;
    }

    public void setVideo(boolean hasVideo) {
        this.hasVideo = hasVideo;
    }

    public boolean getAudio() {
        return hasAudio;
    }

    public void setAudio(boolean hasAudio) {
        this.hasAudio = hasAudio;
    }

    public Activity getActivity() {
        return mActivity;
    }

    public String getApiKey() {
        return mApiKey;
    }

    public MediaConstraints getMediaConstraints(){
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
