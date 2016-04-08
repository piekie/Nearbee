# Nearbee
Small messenger based on Nearby API (Google)

Application demonstrates use of the Nearby.Messages API for messaging between devices in nearby range. In that time Nearby uses WiFi, Bluetooth and ultrasound for this.

Application allows publish a messages to nearby devices and subscribe to broadcasting from those devices.
It publish not a single message and hold it for time but can publish whole sections with the messages. It allows to chat with another devices :)

This application uses the Gradle build system. To build this project, use the "Import Project" in Android Studio.
But you need a permission to use Nearby.Messages API. To access it follow next steps: 
1) Create a project on Google Developer Console. Or, use an existing project.
2) Click on APIs & auth -> APIs, and enable Nearby Messages API.
3) Click on Credentials, then click on Create new key, and pick Android key. Then register your Android app's SHA1 certificate fingerprint and package name for your app.
4) Copy the API key generated, and paste it in AndroidManifest.xml.

In initial commit it uses Nearby icon. Sorry for that. I use it not for the commercial :) It is Google property.
