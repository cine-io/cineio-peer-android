package io.cine.peerclient;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import io.cine.peerclient.receiver.GcmRegisterer;
import io.cine.peerclient.receiver.PlayUnavailableException;

/**
 * Created by thomas on 9/22/14.
 */
public class CinePeerClient {
    private static final String TAG = "CinePeerClient";

    private static CinePeerClientConfig mConfig;
    private final PeerConnectionsManager mPeerConnectionsManager;
    private final SignalingConnection mSignalingConnection;
    private MediaStream lMS;
    private VideoSource videoSource;

    public CinePeerClient(CinePeerClientConfig config) {
        mConfig = config;

        mPeerConnectionsManager = new PeerConnectionsManager(this);
        mSignalingConnection = SignalingConnection.connect(config.getActivity());
        mSignalingConnection.init(config.getApiKey());
        mPeerConnectionsManager.setSignalingConnection(mSignalingConnection);
        mSignalingConnection.setPeerConnectionsManager(mPeerConnectionsManager);
        mSignalingConnection.joinRoom("hello");

    }

    public static CinePeerClient init(CinePeerClientConfig config) throws PlayUnavailableException {
        CinePeerClient c = new CinePeerClient(config);
        c.registerWithCine();
        return c;
    }

    private void registerWithCine() throws PlayUnavailableException {
        GcmRegisterer.registerWithCine(mConfig.getActivity());
    }


    public void end() {
        mSignalingConnection.end();
    }

    public void newIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.v(TAG, "SOME EXTRAS" + extras.toString());
            mSignalingConnection.newMessage(new CineMessage(extras));
        } else {
            Log.v(TAG, "NO extras");
        }

    }

    public void startMediaStream() {

        AudioManager audioManager =
                ((AudioManager) mConfig.getActivity().getSystemService(Context.AUDIO_SERVICE));
        // TODO(fischman): figure out how to do this Right(tm) and remove the
        // suppression.
        @SuppressWarnings("deprecation")
        boolean isWiredHeadsetOn = audioManager.isWiredHeadsetOn();
        audioManager.setMode(isWiredHeadsetOn ?
                AudioManager.MODE_IN_CALL : AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(!isWiredHeadsetOn);

        MediaConstraints constraints = new MediaConstraints();

        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));

        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

        Log.d(TAG, "Creating local video source...");
        PeerConnectionFactory factory = PeerConnectionsManager.getFactory();
        Log.v(TAG, "1");
        MediaConstraints blankMediaConstraints = new MediaConstraints();

        lMS = factory.createLocalMediaStream("ARDAMS");
        Log.v(TAG, "2");
        VideoCapturer capturer = getVideoCapturer();
        Log.v(TAG, "3");
        videoSource = factory.createVideoSource(capturer, blankMediaConstraints);
        Log.v(TAG, "4");
        VideoTrack videoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);
        Log.v(TAG, "5");
        videoTrack.addRenderer(new VideoRenderer(mConfig.getCinePeerRenderer().getLocalRenderer()));
        Log.v(TAG, "6");
        lMS.addTrack(videoTrack);
//        if (appRtcClient.audioConstraints() != null) {
        lMS.addTrack(factory.createAudioTrack(
                "ARDAMSa0",
                factory.createAudioSource(blankMediaConstraints)));
//        }
        mPeerConnectionsManager.setMediaStream(lMS);
    }

    public void runOnUiThread(Runnable action) {
        mConfig.getActivity().runOnUiThread(action);
    }

    // Cycle through likely device names for the camera and return the first
    // capturer that works, or crash if none do.
    private VideoCapturer getVideoCapturer() {
        String[] cameraFacing = {"front", "back"};
        int[] cameraIndex = {0, 1};
        int[] cameraOrientation = {0, 90, 180, 270};
        for (String facing : cameraFacing) {
            Log.v(TAG, "facing: " + facing);
            for (int index : cameraIndex) {
                Log.v(TAG, "index: " + index);
                for (int orientation : cameraOrientation) {
                    Log.v(TAG, "orientation: " + orientation);
                    String name = "Camera " + index + ", Facing " + facing +
                            ", Orientation " + orientation;
                    Log.v(TAG, "name: " + name);
                    VideoCapturer capturer = VideoCapturer.create(name);
                    Log.v(TAG, "got capturer");
                    if (capturer != null) {
                        Log.d(TAG, "Using camera: " + name);
                        return capturer;
                    }
                }
            }
        }
        throw new RuntimeException("Failed to open capturer");
    }

    public SignalingConnection getSignalingConnection() {
        return mSignalingConnection;
    }

    public CinePeerClientConfig getConfig() {
        return mConfig;
    }

    public MediaConstraints getMediaConstraints() {
        return mConfig.getMediaConstraints();
    }

    public void addStream(final MediaStream stream) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stream.videoTracks.get(0).addRenderer(
                        new VideoRenderer(mConfig.getCinePeerRenderer().getRemoteRenderer()));
            }
        });
    }

    public void removeStream(final MediaStream stream) {
        runOnUiThread(new Runnable() {
            public void run() {
                Log.d(TAG, "DISPOSING OF STREAM");
                // causes the app to crash
                // stream.dispose();
                Log.d(TAG, "DISPOSED OF STREAM");
            }
        });

    }
}
