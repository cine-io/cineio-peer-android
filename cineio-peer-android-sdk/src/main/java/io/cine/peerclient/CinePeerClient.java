package io.cine.peerclient;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.Iterator;

import io.cine.peerclient.receiver.GcmRegisterer;
import io.cine.peerclient.receiver.PlayUnavailableException;

/**
 * Created by thomas on 9/22/14.
 */
public class CinePeerClient {
    private static final String TAG = "CinePeerClient";

    public static final String VERSION = "0.0.1";

    private static CinePeerClientConfig mConfig;
    private final PeerConnectionsManager mPeerConnectionsManager;
    private final SignalingConnection mSignalingConnection;
    private MediaStream lMS;
    private VideoSource videoSource;
    private boolean factoryStaticInitialized;
    private VideoCapturer capturer;
    private AudioSource audioSource;

    public CinePeerClient(CinePeerClientConfig config) {
        mConfig = config;
        ensureFactoryGlobals();
        mPeerConnectionsManager = new PeerConnectionsManager(this);
        mSignalingConnection = SignalingConnection.connect(config.getActivity());
        mSignalingConnection.init(config.getApiKey());
        mSignalingConnection.setPeerConnectionsManager(mPeerConnectionsManager);
    }

    public static CinePeerClient init(CinePeerClientConfig config) throws PlayUnavailableException {
        CinePeerClient c = new CinePeerClient(config);
        c.registerWithCine();
        return c;
    }

    private void ensureFactoryGlobals() {
        if (!factoryStaticInitialized) {
            RTCHelper.abortUnless(PeerConnectionFactory.initializeAndroidGlobals(
                            mConfig.getActivity(), true, true),
                    "Failed to initializeAndroidGlobals"
            );
            factoryStaticInitialized = true;
        }

    }

    private void registerWithCine() throws PlayUnavailableException {
        GcmRegisterer.registerWithCine(mConfig.getActivity());
    }


    public void end() {
        //        close our connection to signaling.cine.io
        mSignalingConnection.end();
//        tell the audio manager we are no longer in a call
        getAudioManager().setMode(AudioManager.MODE_NORMAL);

//        dispose of all the local video capture/rendering
//        NOTE: Order is important here. This order seems to work/not crash.
        Log.v(TAG, "disposing video capturer");
        capturer.dispose();
        mPeerConnectionsManager.end();
        Log.v(TAG, "disposing video source");
//        videoSource.dispose();
        videoSource.stop();
//        audioSource.dispose();
        Log.v(TAG, "disposing lms");
//        lMS.dispose();
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

    private AudioManager getAudioManager() {
        return (AudioManager) mConfig.getActivity().getSystemService(Context.AUDIO_SERVICE);
    }

    public void startMediaStream() {

        AudioManager audioManager = getAudioManager();
        // TODO(fischman): figure out how to do this Right(tm) and remove the
        // suppression.
        @SuppressWarnings("deprecation")
        boolean isWiredHeadsetOn = audioManager.isWiredHeadsetOn();
        audioManager.setMode(isWiredHeadsetOn ?
                AudioManager.MODE_IN_CALL : AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(!isWiredHeadsetOn);

        Log.d(TAG, "Creating local video source...");
        PeerConnectionFactory factory = PeerConnectionsManager.getFactory();
        Log.v(TAG, "1");
        MediaConstraints blankMediaConstraints = new MediaConstraints();

        lMS = factory.createLocalMediaStream("ARDAMS");
        Log.v(TAG, "2");
        capturer = getVideoCapturer();
        Log.v(TAG, "3");
        videoSource = factory.createVideoSource(capturer, blankMediaConstraints);
        Log.v(TAG, "4");
        VideoTrack videoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);
        Log.v(TAG, "5");
        videoTrack.addRenderer(new VideoRenderer(mConfig.getCinePeerRenderer().getLocalRenderer()));
        Log.v(TAG, "6");
        lMS.addTrack(videoTrack);
//        if (appRtcClient.audioConstraints() != null) {
        audioSource = factory.createAudioSource(blankMediaConstraints);
        AudioTrack audioTrack = factory.createAudioTrack("ARDAMSa0",audioSource);
        lMS.addTrack(audioTrack);
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
//                I don't actually know if this song and dance is valuable
                Log.d(TAG, "DISPOSING OF STREAM");
                Iterator<AudioTrack> it = stream.audioTracks.iterator();
                while (it.hasNext()) {
                    AudioTrack t = it.next();
                    stream.removeTrack(t);
                }
                Log.d(TAG, "DISPOSING OF STREAM2");
                Iterator<VideoTrack> it2 = stream.videoTracks.iterator();
                while (it2.hasNext()) {
                    VideoTrack t = it2.next();
                    stream.removeTrack(t);
                }
                Log.d(TAG, "DISPOSING OF STREAM3");
                // causes the app to crash
//                stream.dispose();
                Log.d(TAG, "DISPOSED OF STREAM");
            }
        });

    }

    public CinePeerView createView() {
        return new CinePeerView(mConfig.getActivity());
    }

    public void joinRoom(String room) {
        mSignalingConnection.joinRoom(room);
    }
}
