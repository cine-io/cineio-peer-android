package io.cine.peerclient;

import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by thomas on 9/19/14.
 */
public class CineMessage {


    private JSONObject json;
    private Bundle bundle;
    private KIND kind;

    public CineMessage(JSONObject json) {
        this.json = json;
        this.kind = KIND.JSON;
    }

    public CineMessage(Bundle bundle) {
        this.bundle = bundle;
        this.kind = KIND.BUNDLE;
    }

    public String getString(String key) {
        String response = null;
        try {
            switch (this.kind) {
                case JSON:
                    response = json.getString(key);
                    break;
                case BUNDLE:
                    response = bundle.getString(key);
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return response;
    }

    public JSONArray getJSONArray(String key) {
        JSONArray response = null;
        try {
            switch (this.kind) {
                case JSON:
                    response = json.getJSONArray(key);
                    break;
                case BUNDLE:
                    response = new JSONArray(bundle.getString(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return response;

    }

    public JSONObject getJSONObject(String key) {
        JSONObject response = null;
        try {
            switch (this.kind) {
                case JSON:
                    response = json.getJSONObject(key);
                    break;
                case BUNDLE:
                    response = new JSONObject(bundle.getString(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return response;

    }

    public String getAction() {
        return getString("action");
    }

    private static enum KIND {JSON, BUNDLE}


}
