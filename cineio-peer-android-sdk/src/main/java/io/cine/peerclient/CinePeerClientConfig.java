package io.cine.peerclient;

import android.app.Activity;
import android.util.Log;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.webrtc.MediaConstraints;

/**
 * Created by thomas on 9/22/14.
 */
public class CinePeerClientConfig {
    private static final String TAG = "CinePeerClientConfig";

    private final Activity mActivity;
    private final String publicKey;
    private String secretKey;
    private final CinePeerRenderer mCinePeerRenderer;
    private boolean hasVideo;
    private boolean hasAudio;

    public CinePeerClientConfig(String publicKey, Activity activity) {
        mActivity = activity;
        this.publicKey = publicKey;
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

    public String getPublicKey() {
        return publicKey;
    }

    public MediaConstraints getMediaConstraints() {
        MediaConstraints constraints = new MediaConstraints();

        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));

        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("RtpDataChannels", "true"));
//        constraints.optional.add(new MediaConstraints.KeyValuePair("internalSctpDataChannels", "true"));
        Log.d(TAG, "created new constraints");
        return constraints;
    }

    public CinePeerRenderer getCinePeerRenderer() {
        return mCinePeerRenderer;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Identity generateIdentitySignature(String identity) {
        return generateIdentitySignature(identity, this.secretKey);
    }
    public Identity generateIdentitySignature(String identity, String secretKey) {
        long timestamp = System.currentTimeMillis();
        String signatureToSha = "identity="+identity+"&timestamp="+timestamp+secretKey;
        String signature = new String(Hex.encodeHex(DigestUtils.sha(signatureToSha)));
        return new Identity(identity, signature, timestamp);
    }
}
