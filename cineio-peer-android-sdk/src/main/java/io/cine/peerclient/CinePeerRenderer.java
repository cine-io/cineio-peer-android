package io.cine.peerclient;

import org.json.JSONObject;
import org.webrtc.MediaStream;

/**
 * Created by thomas on 9/22/14.
 */
public interface CinePeerRenderer {

    public void mediaAdded(MediaStream stream, boolean local);

    public void mediaRemoved(MediaStream stream, boolean local);

    public void peerData(JSONObject object);

    public void onCall(Call call);

    public void onError(CineMessage message);
}
