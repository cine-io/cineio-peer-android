package io.cine.androidwebsocket;

import org.webrtc.VideoRenderer;

/**
 * Created by thomas on 9/22/14.
 */
public interface CinePeerRenderer{
    public VideoRenderer.Callbacks getLocalRenderer();
    public VideoRenderer.Callbacks getRemoteRenderer();
}
