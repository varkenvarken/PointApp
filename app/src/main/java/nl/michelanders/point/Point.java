package nl.michelanders.point;

import static java.lang.Math.abs;

import org.json.JSONException;
import org.json.JSONObject;

public class Point {
    public String name;
    public String index;
    public int port;
    public boolean enabled;
    public float current;
    public String description;
    public float _left;
    public float _right;
    public float _mid;
    public float speed;
    public String _default; // default is a keyword in java
    public float deltat;
    public String pointtype;

    public String position() {
        String p = "left";
        float d = abs(_left-current);
        float dright = abs(_right-current);
        float dmid = abs(_mid-current);

        if(dright < d){ p = "right"; d = dright; }
        if(dmid < d){ p = "mid";}
        return p;
    }

    public JSONObject toJSON(){
        JSONObject obj = new JSONObject();
        try {
            obj.put("name", name);
            obj.put("index", index);
            obj.put("port", port);
            obj.put("enabled", enabled);
            obj.put("description", description);
            obj.put("_left", _left);
            obj.put("_right", _right);
            obj.put("_mid", _mid);
            obj.put("speed", speed);
            obj.put("deltat", deltat);
            obj.put("default", _default);
            obj.put("pointtype", pointtype);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
