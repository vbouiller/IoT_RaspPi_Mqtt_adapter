package mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static com.sun.xml.internal.org.jvnet.fastinfoset.FastInfosetSerializer.UTF_8;

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
                    System.out.println(topic + ": " + message.toString());//Arrays.toString(message.getPayload()));
                    handleNewMessage(topic, Arrays.toString(message.getPayload()));
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
                    "Successfully connected".getBytes(UTF_8), // payload
                    2, // QoS
                    false); // retained?
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    protected static void handleNewMessage(String topic, String message){

    }


}
