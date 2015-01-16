package io.cine.peerclient;

/**
 * Created by thomas on 1/16/15.
 */
public interface CallHandler {
    public void onCancel(String identity);
    public void onReject(String identity);
}
