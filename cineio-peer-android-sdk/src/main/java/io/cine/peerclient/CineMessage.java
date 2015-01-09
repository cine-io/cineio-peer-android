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
        this.kind = KIND.BUDNLE;
    }

    public String getString(String key) {
        String response = null;
        try {
            switch (this.kind) {
                case JSON:
                    response = json.getString(key);
                    break;
                case BUDNLE:
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
                case BUDNLE:
                    // TODO: NOT IMPLEMENTED
                    throw new RuntimeException("NOT IMPLEMENTED");
//                   response = bundle.getStringArray(key);
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
                case BUDNLE:
                    // TODO: NOT IMPLEMENTED
                    throw new RuntimeException("NOT IMPLEMENTED");
//                   response = bundle.getStringArray(key);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return response;

    }

    public String getAction() {
        return getString("action");
    }

    private static enum KIND {JSON, BUDNLE}


}
