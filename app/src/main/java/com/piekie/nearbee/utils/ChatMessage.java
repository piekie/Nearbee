package com.piekie.nearbee.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

/**
 * Used to handle a message:
 * static Gson Object
 * generated ID
 * text of the message
 */

public class ChatMessage {
    public static final Gson gson = new Gson();

    /**
     * Used to identify messages while parsing
     * Messages goes to client. Client checks does we have this message via ID check
     * in onFound method in messageListener
     */
    private long ID;

    /**
     * Text of the message
     */
    public String text;

    public ChatMessage(Activity activity, String text) {
        identify(activity);
        this.text = text;
    }

    /**
     * @return ID - identifier of the message
     */
    public long getID() {
        return ID;
    }

    /**
     * Set a ID to message (this). Take last ID from preferences and set a +1 to that
     *
     * @param activity - activity that called the method : for accessing to SharedPreferences
     */
    protected void identify(Activity activity) {
        // Getting lastID via activity.getPreferences()
        long lastID = activity.getPreferences(Context.MODE_PRIVATE)
                .getLong("MessageID", 0);

        //If lastID almost is MAX_VALUE
        if (lastID == Long.MAX_VALUE - 1) {
            // Make it 0
            ID = 0;
        } else {
            // Increment lastID and setting it to this.ID
            ID = lastID + 1;
        }

        Log.i("NearbyFragment", "identifying: " + Long.toString(ID));

        //Saving new ID as a lastID
        activity.getPreferences(Context.MODE_PRIVATE)
                .edit()
                .putLong("MessageID", ID)
                .apply();
    }
}