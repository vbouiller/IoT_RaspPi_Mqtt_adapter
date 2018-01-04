package mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import philipshue.PhilipshueRequester;

import java.io.UnsupportedEncodingException;

import static philipshue.PhilipshueRequester.PutToPHue;

public class Main {

    protected static  MqttClient client = null;

    public static void main(String[] args) {

        String topic = "#";
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
                    System.out.println("-- Connection List");
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

            client.publish(
                    "RPi/MqttAdapter/connexion", // topic
                    "Successfully connected - From jar".getBytes(), // payload
                    2, // QoS
                    false); // retained?
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    protected static void handleNewMessage(String topic, String message){
        System.out.println(topic + ": " + message);

        String objectType = topic.substring(0,topic.indexOf("/"));
        String objectName = topic.substring(topic.indexOf("/")+1);
        System.out.println("CUT: "+objectType +" -- "+objectName);

        if (objectType.equalsIgnoreCase("philipshue")){
            if(message.equalsIgnoreCase("on"))
                //Switch light on
                PhilipshueRequester.PutToPHue(objectName,"on");
            else if (message.equalsIgnoreCase("off"))
                PhilipshueRequester.PutToPHue(objectName,"off");

        }

    }


}
