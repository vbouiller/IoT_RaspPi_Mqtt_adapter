package mqtt;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import actuatorRequesters.PhilipshueRequester;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    protected static  MqttClient client = null;
    static String philipsHueBridgeIP;
    static String philipsHueUsername;



    public static void main(String[] args) {

        String topic = "#";
        if (args.length >= 1) {
            philipsHueBridgeIP =  args[0]; //"192.168.0.134";
        } else {
            philipsHueBridgeIP =  "192.168.0.134";
        }
        philipsHueUsername = philipsHueKeyRetriever(philipsHueBridgeIP);
        JSONArray PHueLights = PhilipshueRequester.GetPHueLights(philipsHueBridgeIP, philipsHueUsername);

        mqttClientConnection(client, topic);

    }



    //Handles MQTT Client connection, Callback setup, and subscription
    private static void mqttClientConnection(MqttClient mqttClient, String topic){

        try {

            //Instantiate MqttClient
            client = new MqttClient(
                    "tcp://m23.cloudmqtt.com:13655", //URI
                    MqttClient.generateClientId(), //ClientId
                    new MemoryPersistence()); //Persistence

            // Connection credentials
            MqttConnectOptions options = new MqttConnectOptions();
                /*options.setUserName("mxasjcaz");
                options.setPassword("6YcXZea6Z1eu".toCharArray());*/
            options.setUserName("mqttJavaAdapter");
            options.setPassword("kDzh7ida!nr".toCharArray());


            // Callback triggered when receiving a new message
            client.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable cause) { //Called when the client lost the connection to the broker
                    System.out.println("-- Connection Lost");
                    cause.printStackTrace();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    //System.out.println(topic + ": " + message.toString());
                    handleNewMessage(topic, message.toString());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {//Called when a outgoing publish is complete
                    System.out.println("-- Message sent w/ token: " + token);
                }
            });


            client.connect(options);
            client.subscribe("#");

        } catch (MqttException e) {
            e.printStackTrace();
        }


        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            client.publish(
                    "RPi/MqttAdapter/connexion", // topic
                    ("Successfully connected - From "+ hostname).getBytes(), // payload
                    2, // QoS
                    false); // retained?
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    //Handles MQTT message and select appropriate method to use according to mqtt topic and message
    protected static void handleNewMessage(String topic, String message){
        System.out.println(topic + ": " + message);

        if (topic != null || !topic.equalsIgnoreCase("")) {
            String objectType = null;
            String objectName = null;

            if (topic.contains("/")){
                objectType = topic.substring(0,topic.indexOf("/"));
                objectName = topic.substring(topic.indexOf("/")+1);
            } else {
                objectType = topic;
                objectName = "";
            }

            if (objectType.equalsIgnoreCase("philipshue")){
                System.out.println("== Philips Hue request:");
                if(message.equalsIgnoreCase("on")) {
                    //Switch light on
                    System.out.println("==== Turn on light "+objectName);
                    PhilipshueRequester.PutToPHue(philipsHueBridgeIP, philipsHueUsername, objectName, true);
                }
                else if (message.equalsIgnoreCase("off")) {
                    //Switch light off
                    System.out.println("==== Turn off light "+objectName);
                    PhilipshueRequester.PutToPHue(philipsHueBridgeIP, philipsHueUsername,objectName, false);
                }
                else if (message.equalsIgnoreCase("refresh")){
                    System.out.println("==== Searching for PHue Lights ");
                    PhilipshueRequester.GetPHueLights(philipsHueBridgeIP,philipsHueUsername);
                }

            }
        }

    }

    //Handles connection to Bridge and retrieve username from the api
    private static String philipsHueKeyRetriever(String bridgeIP){
        String url = PhilipshueRequester.PHUE_PROTOCOL + bridgeIP + PhilipshueRequester.PHUE_BRIDGE_API;    //URL to query
        String username = "";    //String to be returned
        JSONObject keyRequestJSON = new JSONObject();   //JSON for the request
        JSONArray keyAnswerJSON;    //JSON for the answer
        //HTTP client and response
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        CloseableHttpResponse answer1 = null;
        boolean cont = true;    //Boolean to continue trying to retrieve username


        try {
            keyRequestJSON.put("devicetype","our_iot_project_Daoud_Bouiller");
        } catch (JSONException e) {
            e.printStackTrace();
        }



        try {
            //Create and set parameters of request
            HttpPost request = new HttpPost(url);
            StringEntity params = new StringEntity(keyRequestJSON.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity(params);

            HttpEntity entity1;
            Long tic = System.currentTimeMillis();
            Long tac = tic;
            //On boucle pour envoyer les requests tant qu'on n'a pas de username
            do {
                answer1 = httpClient.execute(request);
                entity1 = answer1.getEntity();
                keyAnswerJSON = new JSONArray(EntityUtils.toString(entity1));

                //If request contains the "success" object, then we have the username
                if (keyAnswerJSON.getJSONObject(0).has("success")) {
                    System.out.println("=-| Username retrieved: " + keyAnswerJSON.getJSONObject(0).getJSONObject("success").getString("username"));
                    cont = false; //We stop the loop
                    username = keyAnswerJSON.getJSONObject(0).getJSONObject("success").getString("username");

                } else { //otherwise, we continue and ask to press the button
                    Long timeSinceLastPrint = System.currentTimeMillis() - tac;
                    Long timeSinceLoopBeginning = System.currentTimeMillis() - tic;
                    if (timeSinceLoopBeginning < 700 ||timeSinceLastPrint > 2000) {
                        System.out.println(" /!\\ Please press link button /!\\ ");
                        tac = System.currentTimeMillis();

                    }

                }

                Long toc = System.currentTimeMillis();
                //Si l'on essaye d'obtenir la cle depuis 5 minutes, on arrête d'essayer.
                if (toc-tic > 1000*60*5) {
                    cont = false;
                    System.out.println("Link button not pressed within 5 minutes - Stopping automatic association");
                }
                Thread.sleep(700);
            } while (cont == true);


            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(entity1);
        } catch (Exception ex) {
            // handle exception here
            ex.printStackTrace();
        } finally {
            try {
                answer1.close();
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return username;
    }

    //Sends JSONArray of lights: id, level and status to the DB
    private static void sendToAPIPHueLights(JSONArray PHueLights){
        String url = "https://pure-basin-20770.herokuapp.com/api/rooms/PHueRefresh"; //API URL


        //HTTP client and response
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();


        try {
            //Create and set parameters of request
            HttpPost request = new HttpPost(url);
            StringEntity params = new StringEntity(PHueLights.toString());
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
