package io.cine.cineiopeerclientexampleapp.exampleapp;

import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;

/**
 * Created by thomas on 1/13/15.
 */
public class MediaStreamAndRenderer {

    private final boolean local;
    private MediaStream mediaStream;
    private VideoRenderer videoRenderer;

//    public MediaStreamAndRenderer(MediaStream mediaStream, VideoRenderer videoRenderer) {
//        this.mediaStream = mediaStream;
//        this.videoRenderer = videoRenderer;
//    }


    public MediaStreamAndRenderer(MediaStream mediaStream, boolean local) {
        this.mediaStream = mediaStream;
        this.local = local;
    }

    public VideoRenderer getVideoRenderer() {
        return videoRenderer;
    }

    public void setVideoRenderer(VideoRenderer videoRenderer) {
        this.videoRenderer = videoRenderer;
        getVideoTrack().addRenderer(getVideoRenderer());
    }

    public MediaStream getMediaStream() {
        return mediaStream;
    }

    public void setMediaStream(MediaStream mediaStream) {
        this.mediaStream = mediaStream;
    }

    public VideoTrack getVideoTrack() {
        return this.mediaStream.videoTracks.getFirst();
    }

    public void removeVideoRenderer() {
        if(getVideoRenderer() != null){
            getVideoTrack().removeRenderer(getVideoRenderer());
        }
        this.videoRenderer = null;
    }

    public boolean isLocal() {
        return local;
    }
}
