package zm.swiatlo;

import android.os.Build;
import android.util.JsonReader;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import http.websocket.client.WebSocketClient;
import http.websocket.handshake.ServerHandshake;

/**
 * Created by User on 2015-12-01.
 */
public class WsClient extends WebSocketClient {

    public final static List< Group> groups = new LinkedList<>();

    public WsClient() throws URISyntaxException {
        super(new URI("ws://192.168.1.10:8887"));
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        Log.i("Websocket", "Opened");
        //   send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
        send("{action:\"getGroups\"}");
    }

    @Override
    public void onMessage(String s) {
        final String message = s;

        try {


            JSONObject json = new JSONObject(s);

            JSONObject result = json.getJSONObject("result");

            Iterator<String> itr = result.keys();
            while (itr.hasNext()) {
                JSONObject obj = result.getJSONObject(itr.next());

                Group group = new Group();
                group.id = obj.getInt("id");
                group.name = obj.getString("name");
                group.value = obj.getInt("value");
                JSONArray arr = obj.getJSONArray("channels");

                for (int i = 0; i < arr.length(); i++)
                    group.terminals.add(arr.getString(i));

                groups.add(group);

            }
            synchronized (groups) {
                groups.notifyAll();
            }

        } catch (JSONException e) {
            Log.e("WebSocket", e.toString());
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        Log.i("Websocket", "Closed " + s);
    }

    @Override
    public void onError(Exception e) {
        Log.i("Websocket", "Error " + e.getMessage());
    }

    public class Group {

        public int id;
        public String name;

        public final List<String> terminals = new LinkedList<>();
        public int value;
    }
}

