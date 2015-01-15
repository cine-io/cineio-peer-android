package io.cine.peerclient;

import android.util.Log;

import org.webrtc.DataChannel;
import org.webrtc.PeerConnection;

/**
 * Created by thomas on 9/15/14.
 */
public class RTCMember {

    private static final String TAG = "RTCMember";
    private String sparkUUID;
    private String sparkId;
    private PeerConnection peerConnection;
    private PeerObserver peerObserver;
    private DataChannel mainDataChannel;

    public RTCMember(String sparkUUID) {
        Log.v(TAG, "Creating rtc member");
        this.sparkUUID = sparkUUID;
    }

    public PeerConnection getPeerConnection() {
        return peerConnection;
    }

    public void setPeerConnection(PeerConnection peerConnection) {
        this.peerConnection = peerConnection;
    }

    public PeerObserver getPeerObserver() {
        return peerObserver;
    }

    public void setPeerObserver(PeerObserver peerObserver) {
        this.peerObserver = peerObserver;
    }


    public void close() {
        PeerConnection pc = getPeerConnection();
        PeerObserver observer = getPeerObserver();
        DataChannel channel = getMainDataChannel();
        if (channel != null) {
            channel.close();
        }
        if (observer != null) {
            observer.dispose();
        }
        if (pc != null) {
            pc.close();
            // causes the app to crash
            // pc.dispose();
        }
    }

    public String getSparkId() {
        return sparkId;
    }

    public void setClientSparkId(String clientSparkId) {
        this.sparkId = clientSparkId;
    }

    public void setMainDataChannel(final DataChannel mainDataChannel) {
        this.mainDataChannel = mainDataChannel;
        this.mainDataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onStateChange() {
                Log.v(TAG, "Got new state");
                Log.v(TAG, "NEW STATE: " + mainDataChannel.state().name());
                //TODO, send pending messages
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                Log.v(TAG, "Got new message");
            }
        });
    }

    public DataChannel getMainDataChannel() {
        return mainDataChannel;
    }

}
