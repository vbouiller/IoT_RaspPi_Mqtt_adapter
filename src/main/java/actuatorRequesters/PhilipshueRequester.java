package actuatorRequesters;


import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class PhilipshueRequester {

    //Definition strings
    public static final String PHUE_PROTOCOL = "http://";
    public static final String PHUE_BRIDGE_API = "/api/";
    private static final String PHUE_LIGHT = "lights/";
    private static final String PHUE_LIGHT_STATE = "state/";



    public PhilipshueRequester() {

    }


    public static void PutToPHue(String PhilipsHueBridgeIP, String PhilipsHueUsername, String lightId, String statusToPut) {
        //String TEST_ON_API = "https://pure-basin-20770.herokuapp.com/api/rooms/1/switch/light";
        String url = PHUE_PROTOCOL + PhilipsHueBridgeIP + PHUE_BRIDGE_API + PhilipsHueUsername + PHUE_LIGHT + lightId + PHUE_LIGHT_STATE;

        JSONObject json = new JSONObject();
        try {
            json.put("on", statusToPut);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        /*json.put("sat", "someValue");
        json.put("bri", "someValue");
        json.put("hue", "someValue");*/

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        try {
            HttpPost request = new HttpPost(url);
            StringEntity params = new StringEntity(json.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity(params);
            httpClient.execute(request);
        } catch (Exception ex) {
            // handle exception here
            ex.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}