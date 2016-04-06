package com.piekie.nearbee.utils;

/**
 * User credentials wrapper
 */
public class Profile {

    /**
     * InstanceID. ID of the device
     */
    private final String instanceID;
    /**
     * Name of the profile
     */
    public String name;

    public Profile(String instanceID, String name) {
        this.instanceID = instanceID;
        this.name = name;
    }
    public String getID() {
        return instanceID;
    }
}
