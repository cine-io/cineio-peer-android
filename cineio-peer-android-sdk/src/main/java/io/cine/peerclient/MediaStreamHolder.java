package io.cine.peerclient;

import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;

/**
 * Created by thomas on 1/9/15.
 */
public class MediaStreamHolder {

    private final MediaStream stream;
    private VideoRenderer renderer = null;

    public MediaStreamHolder(MediaStream stream) {
        this.stream = stream;
    }

    public void addRenderer(VideoRenderer renderer) {
        this.renderer = renderer;
    }
    public VideoRenderer getRenderer(){
        return this.renderer;
    }

    public MediaStream getStream() {
        return stream;
    }
}
