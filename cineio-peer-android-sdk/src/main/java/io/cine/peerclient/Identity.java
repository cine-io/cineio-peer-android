package io.cine.peerclient;

/**
 * Created by thomas on 1/16/15.
 */
public class Identity {
    private final String identity;
    private final String signature;
    private final long timestamp;

    public Identity(String identity, String signature, long timestamp) {
        this.identity = identity;
        this.signature = signature;
        this.timestamp = timestamp;
    }

    public String getIdentity() {
        return identity;
    }

    public String getSignature() {
        return signature;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
