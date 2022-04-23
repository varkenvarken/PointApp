package nl.michelanders.point;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AuthJsonObjectRequest extends JsonObjectRequest{
    protected String username;
    protected String password;

    public AuthJsonObjectRequest(String username, String password, int method, String url, @Nullable JSONObject jsonRequest, Response.Listener<JSONObject> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
        this.username = username;
        this.password = password;
    }

    @Override
    public Map<String, String> getHeaders(){
        String text = username + ":" + password;

        byte[] data = null;
        try {
            data = text.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        String base64 = Base64.encodeToString(data, Base64.DEFAULT);
        //  Authorization: Basic $auth
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Basic "+base64);
        return headers;
    }
}
