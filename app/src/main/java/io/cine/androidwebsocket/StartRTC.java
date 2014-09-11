package io.cine.androidwebsocket;

import android.util.Log;

import com.koushikdutta.async.http.WebSocket;

import org.webrtc.AudioTrack;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.ArrayList;

/**
 * Created by thomas on 9/11/14.
 */
public class StartRTC {
    private static final String TAG = "StartRTC";

    private ArrayList<PeerConnection.IceServer> servers;
    private PeerConnectionFactory factory;
    private MediaConstraints constraints;
    private WebSocket mWebSocket;

    public StartRTC(){
        servers = new ArrayList<PeerConnection.IceServer>();
    }
    public void setSignalingConnection(WebSocket ws){
        this.mWebSocket = ws;
    }
    public void addIceServer(String iceServer){
        servers.add(new PeerConnection.IceServer(iceServer));
    }


    public void start() {
        factory = new PeerConnectionFactory();

        constraints = new MediaConstraints();

        PeerObserver observer = new PeerObserver();
        PeerConnection peerConnection = factory.createPeerConnection(servers, constraints, observer);

        MediaStream mediaStream = factory.createLocalMediaStream("ARDAMS");
        AudioTrack track = factory.createAudioTrack("ARDAMSa0", factory.createAudioSource(constraints));
        mediaStream.addTrack(track);
        peerConnection.addStream(mediaStream, constraints);

        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                Log.d(TAG, "onCreateSuccess");
            }

            @Override
            public void onSetSuccess() {
                Log.d(TAG, "onSetSuccess");
            }

            @Override
            public void onCreateFailure(String error) {
                Log.d(TAG, "onCreateFailure");
            }

            @Override
            public void onSetFailure(String error) {
                Log.d(TAG, "onSetFailure");
            }
        }, constraints);

    }
}
