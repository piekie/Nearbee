package com.piekie.nearbee.utils;

import com.google.android.gms.nearby.messages.Message;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

/**
 * "Wrapper" to store user's credentials and messages for sending
 */
public class Packet {

    private Profile profile;
    /**
     * HashMap: id of message - message
     */
    private HashMap<Long, ChatMessage> messages;

    /**
     * @return entire HashMap with messages
     */
    public HashMap<Long, ChatMessage> getMessages() {
        return messages;
    }

    /**
     * Constructor
     * @param p profile to send
     * @param cm queue to parse and cast to HashMap (take IDs as keys)
     */
    public Packet(Profile p, Queue<ChatMessage> cm) {
        this.profile = p;
        messages = new HashMap<>();

        //Cast queue to ArrayList for the better iterating
        List<ChatMessage> cms = new ArrayList<>(cm);

        //Go through all the list and add to a HashMap
        for (ChatMessage message : cms) {
            messages.put(message.getID(), message);
        }
    }

    /**
     * Parse message and creating the Packet
     * @param message {@link Message} to parse
     * @return formed Packet for sending
     */
    public static Packet parseMessage(Message message) {
        //Trimming (deleting spaces)
        String nearbyMessageString = new String(message.getContent()).trim();

        //Convert from JSON to Packet
        return ChatMessage.gson.fromJson(
                (new String(nearbyMessageString.getBytes(Charset.forName("Cp1251")))),
                Packet.class);
    }

    /**
     * Parse the params and return a message
     * @param p {@link Profile} credentials
     * @param messages queue of the messages
     * @return builded Message
     */
    public static Message build(Profile p, Queue<ChatMessage> messages) {
        Packet packet = new Packet(p, messages);
        return new Message(ChatMessage.gson.toJson(packet).getBytes(Charset.forName("Cp1251")));
    }

    public String getPacketProfileID() {
        return profile.getID();
    }

    public String getPacketProfileName() {
        return profile.name;
    }
}
