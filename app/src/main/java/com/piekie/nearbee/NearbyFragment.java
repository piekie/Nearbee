package com.piekie.nearbee;

import android.app.Fragment;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.NearbyMessagesStatusCodes;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.piekie.nearbee.adapters.ChatAdapter;
import com.piekie.nearbee.utils.ChatMessage;
import com.piekie.nearbee.utils.Constants;
import com.piekie.nearbee.utils.OneComment;
import com.piekie.nearbee.utils.Packet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class NearbyFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "NearbyFragment";

    /**
     * Creating of strategy (duration) for published message and subscription to live.
     * publication - Default (5 mins)
     * subscription - Infinite (max)
     */
    private static final Strategy PUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(Strategy.TTL_SECONDS_DEFAULT).build();
    private static final Strategy SUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(Strategy.TTL_SECONDS_INFINITE).build();

    // Views
    private EditText messageView;
    private TextView emptyChatTV;
    private TextView nearbyAmount;
    private ImageButton refreshButton;

    /**
     * Stack of the messages that was sent.
     */
    private Queue<ChatMessage> mStack;
    /**
     * HashMap: sender - IDs of messages he sent.
     */
    private HashMap<String, Set<Long>> allMessagesFrom;

    /**
     * Current message we broadcast
     */
    private Message message;

    /**
     * Adapter to link listView with stack of messages.
     */
    private ChatAdapter chatAdapter;
    /**
     * Provides an entry point for Google Play services.
     */
    private GoogleApiClient mGoogleApiClient;
    /**
     * A {@link MessageListener} for processing messages from nearby devices.
     */
    private MessageListener mMessageListener;
    /**
     * Tracks if we are currently resolving an error related to Nearby permissions. Used to avoid
     * duplicate Nearby permission dialogs if the user initiates both subscription and publication
     * actions without having opted into Nearby.
     */
    private boolean mResolvingNearbyPermissionError = false;

    private boolean isRefreshing = false;

    public NearbyFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use a retained fragment to avoid re-publishing or re-subscribing upon orientation
        // changes.
        setRetainInstance(true);

        mStack = new LinkedList<>();
        allMessagesFrom = new HashMap<>();
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().getPreferences(Context.MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        updateSharedPreference(Constants.KEY_SUBSCRIPTION_TASK,
                Constants.TASK_SUBSCRIBE);
    }

    @Override
    public void onStop() {
        if (mGoogleApiClient.isConnected() && !getActivity().isChangingConfigurations()) {
            // Using Nearby is battery intensive. To preserve battery, stop subscribing or
            // publishing when the fragment is inactive.
            unsubscribe();
            unpublish();
            updateSharedPreference(Constants.KEY_SUBSCRIPTION_TASK, Constants.TASK_NONE);
            updateSharedPreference(Constants.KEY_PUBLICATION_TASK, Constants.TASK_NONE);

            mGoogleApiClient.disconnect();
            getActivity().getPreferences(Context.MODE_PRIVATE)
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View wrapper = inflater.inflate(R.layout.fragment_main, container, false);


        refreshButton = (ImageButton) wrapper.findViewById(R.id.refresh_chat);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRefreshing) {
                    String subscriptionTask = getPubSubTask(Constants.KEY_SUBSCRIPTION_TASK);
                    if (TextUtils.equals(subscriptionTask, Constants.TASK_NONE) ||
                            TextUtils.equals(subscriptionTask, Constants.TASK_UNSUBSCRIBE)) {
                        updateSharedPreference(Constants.KEY_SUBSCRIPTION_TASK, Constants.TASK_SUBSCRIBE);
                    } else {
                        updateSharedPreference(Constants.KEY_SUBSCRIPTION_TASK,
                                Constants.TASK_UNSUBSCRIBE);

                        //Setting of animation when is clicked.
                        Animation rotation = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.rotate_refresh);
                        refreshButton.startAnimation(rotation);

                        //Refreshing flag is true. For subscribe() function.
                        isRefreshing = true;
                    }
                }
            }
        });

        emptyChatTV = (TextView) wrapper.findViewById(R.id.chat_empty_title);
        nearbyAmount = (TextView) wrapper.findViewById(R.id.nearby_amount);

        ListView chatView = (ListView) wrapper.findViewById(R.id.chat);
        chatAdapter = new ChatAdapter(getActivity().getApplicationContext(), R.id.chat);
        //Setting adapter to listView (chat).
        chatView.setAdapter(chatAdapter);

        mMessageListener = new MessageListener() {

            /** If we see any broadcasting we cast it to ChatMessage
             *  @param messageCaught - a message we caught :)
             */
            @Override
            public void onFound(final Message messageCaught) {

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Forming packet from message we got
                        Packet packet = Packet.parseMessage(messageCaught);
                        //Getting all the messages from the packet
                        HashMap<Long, ChatMessage> contentOfPacket = packet.getMessages();
                        //Getting keys of the messages
                        Set<Long> mIDs = contentOfPacket.keySet();

                        /**
                         * IDs of messages of some "sender"
                         */
                        Set<Long> list;

                        // Check if we already got any message from that sender. By packet.ID
                        if (allMessagesFrom.containsKey(packet.getPacketProfileID())) {
                            //True: take this set to work with
                            list = allMessagesFrom.get(packet.getPacketProfileID());
                            Log.i(TAG, "handling input: we have this sender - " + list.toString());
                        } else {
                            //False: We create new set
                            list = new HashSet<>();

                            //Add to nearbyAmount (number of devices) one more
                            int prevAmount = Integer.parseInt(nearbyAmount.getText().toString());
                            nearbyAmount.setText(String.valueOf(prevAmount + 1));
                        }

                        //Delete all the entries of messages we got already (by IDs)
                        mIDs.removeAll(list);

                        //Add new to list of IDs of sender
                        list.addAll(mIDs);
                        allMessagesFrom.put(packet.getPacketProfileID(), list);

                        //Toast with name of sender
                        Toast.makeText(getActivity().getApplicationContext(),
                                "Got new messages from " + packet.getPacketProfileName(),
                                Toast.LENGTH_SHORT)
                                .show();

                        //Add every message to chatAdapter to display
                        for (Long key : mIDs) {
                            chatAdapter.add(new OneComment(true, contentOfPacket.get(key).text));
                        }

                        //If message that tells us that we did not got any message is visible
                        if (emptyChatTV.getVisibility() == View.VISIBLE) {
                            //Remove it
                            emptyChatTV.setVisibility(View.GONE);
                        }
                    }
                });
            }
        };




        /* MessageView */
        messageView = (EditText) wrapper.findViewById(R.id.chat_message);

        /* SendMessageButton */
        ImageButton sendMessageButton = (ImageButton) wrapper.findViewById(R.id.chat_send);
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //If field with our message is not empty
                if (!messageView.getText().toString().equals("")) {
                    //If message that tells us that we did not got any message is visible
                    if (emptyChatTV.getVisibility() == View.VISIBLE) {
                        //Remove it
                        emptyChatTV.setVisibility(View.GONE);
                    }
                    //Handling the input
                    handleMessage();
                }
            }
        });

        return wrapper;
    }

    /**
     * Handling the input:
     * parsing it, casting to {@link ChatMessage},
     * adding to adapter and sending
     */
    private void handleMessage() {
        //Parsing the input.
        ChatMessage chatMessage = new ChatMessage(getActivity(), messageView.getText().toString());
        //Clear the "chat" EditText
        messageView.setText("");

        chatAdapter.add(new OneComment(false, chatMessage.text));

        //Adding to stack of messages we send
        mStack.add(chatMessage);

        //If size of Message > 3kb we poll the messages (deleting first sent) till it will be < 3kb
        while (Packet.build(ChatActivity.myProfile, mStack).toString().getBytes().length >= 3000) {
            mStack.poll();
        }

        //Creating the packet. Consists sender credentials and messages
        message = Packet.build(ChatActivity.myProfile, mStack);

        Log.i(TAG, "handling: message was built");

        String publicationTask = getPubSubTask(Constants.KEY_PUBLICATION_TASK);
        if (TextUtils.equals(publicationTask, Constants.TASK_NONE) ||
                TextUtils.equals(publicationTask, Constants.TASK_UNPUBLISH)) {
            updateSharedPreference(Constants.KEY_PUBLICATION_TASK, Constants.TASK_PUBLISH);
        } else {
            updateSharedPreference(Constants.KEY_PUBLICATION_TASK,
                    Constants.TASK_UNPUBLISH);
        }
    }

    protected void finishedResolvingNearbyPermissionError() {
        mResolvingNearbyPermissionError = false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
        // If the user has requested a subscription or publication task that requires
        // GoogleApiClient to be connected, we keep track of that task and execute it here, since
        // we now have a connected GoogleApiClient.
        executePendingTasks();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended: "
                + connectionSuspendedCauseToString(cause));
    }

    private static String connectionSuspendedCauseToString(int cause) {
        switch (cause) {
            case CAUSE_NETWORK_LOST:
                return "CAUSE_NETWORK_LOST";
            case CAUSE_SERVICE_DISCONNECTED:
                return "CAUSE_SERVICE_DISCONNECTED";
            default:
                return "CAUSE_UNKNOWN: " + cause;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.i(TAG, "connection to GoogleApiClient failed");
    }

    /**
     * Based on values stored in SharedPreferences, determines the subscription or publication task
     * that should be performed.
     */
    private String getPubSubTask(String taskKey) {
        return getActivity()
                .getPreferences(Context.MODE_PRIVATE)
                .getString(taskKey, Constants.TASK_NONE);
    }


    /**
     * Subscribes to messages from nearby devices. If not successful, attempts to resolve any error
     * related to Nearby permissions by displaying an opt-in dialog. Registers a callback which
     * updates state when the subscription expires.
     */
    private void subscribe() {
        Log.i(TAG, "subscribing: trying to subscribe");

        // Cannot proceed without a connected GoogleApiClient. Reconnect and execute the pending
        // task in onConnected().
        if (!mGoogleApiClient.isConnected()) {
            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else {
            SubscribeOptions options = new SubscribeOptions.Builder()
                    .setStrategy(SUB_STRATEGY)
                    .setCallback(new SubscribeCallback() {
                        @Override
                        public void onExpired() {
                            super.onExpired();
                            Log.i(TAG, "no longer subscribing");
                            updateSharedPreference(Constants.KEY_SUBSCRIPTION_TASK,
                                    Constants.TASK_NONE);
                        }
                    }).build();

            Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options)
                    .setResultCallback(new ResultCallback<Status>() {

                        @Override
                        public void onResult(@NonNull Status status) {
                            Log.i(TAG, "subscribing: SUCCESS");
                            if (status.isSuccess()) {
                                //If we trying to subscribe during refreshing
                                if (isRefreshing) {
                                    Log.d(TAG, "subscibing: REFRESH");
                                    Toast.makeText(getActivity().getApplicationContext(), "Resubscribed successfully :)", Toast.LENGTH_SHORT).show();

                                    //Stop the animation
                                    refreshButton.getAnimation().setRepeatCount(1);
                                    //Flag of refreshing to false
                                    isRefreshing = false;
                                }
                            } else {
                                Log.d(TAG, "subscibing: FAILURE");
                                Toast.makeText(getActivity().getApplicationContext(), "Something went wrong during subscribing :(", Toast.LENGTH_SHORT).show();
                                handleUnsuccessfulNearbyResult(status);
                            }
                        }
                    });
        }
    }

    /**
     * Ends the subscription to messages from nearby devices. If successful, resets state. If not
     * successful, attempts to resolve any error related to Nearby permissions by
     * displaying an opt-in dialog.
     */
    private void unsubscribe() {
        Log.i(TAG, "trying to unsubscribe");
        // Cannot proceed without a connected GoogleApiClient. Reconnect and execute the pending
        // task in onConnected().
        if (!mGoogleApiClient.isConnected()) {
            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else {
            Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener)
                    .setResultCallback(new ResultCallback<Status>() {

                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()) {
                                Log.i(TAG, "unsubscribed successfully");

                                //If we unsubscribe because we want to refresh we add a subscribe task to pending
                                if (isRefreshing) {
                                    updateSharedPreference(Constants.KEY_SUBSCRIPTION_TASK,
                                            Constants.TASK_SUBSCRIBE);
                                }
                            } else {
                                Log.i(TAG, "could not unsubscribe");
                                handleUnsuccessfulNearbyResult(status);
                            }
                        }
                    });
        }
    }

    private void publish() {
        if (mStack.size() != 0) {
            Log.i(TAG, "publish: trying to send a message");

            // Cannot proceed without a connected GoogleApiClient. Reconnect and execute the pending
            // task in onConnected().
            if (!mGoogleApiClient.isConnected()) {
                if (!mGoogleApiClient.isConnecting()) {
                    mGoogleApiClient.connect();

                }
            } else {
                PublishOptions options = new PublishOptions.Builder()
                        .setStrategy(PUB_STRATEGY)
                        .setCallback(new PublishCallback() {
                            @Override
                            public void onExpired() {
                                super.onExpired();
                                Log.i(TAG, "publish: no longer publishing");
                                updateSharedPreference(Constants.KEY_PUBLICATION_TASK,
                                        Constants.TASK_NONE);
                                //Clearing the stack of messages.
                                //Think that all the messages we send went to somebody :)
                                mStack.clear();
                            }
                        }).build();

                /** Trying to publish
                 *  client - need it, our message, strategy above
                 *  Also we create function to execute as a result of publishing
                 */
                Nearby.Messages.publish(mGoogleApiClient, message, options)
                        .setResultCallback(new ResultCallback<Status>() {

                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    Log.i(TAG, "publish: sending was successful");
                                } else {
                                    Log.i(TAG, "publish: could not send");
                                    handleUnsuccessfulNearbyResult(status);
                                }
                            }
                        });
            }
        }
    }

    private void unpublish() {
        if (mStack.size() != 0) {
            Log.i(TAG, "unpublish: trying to unpublish");

            if (!mGoogleApiClient.isConnected()) {
                if (!mGoogleApiClient.isConnecting()) {
                    mGoogleApiClient.connect();
                }
            } else {
                Nearby.Messages.unpublish(mGoogleApiClient, message)
                        .setResultCallback(new ResultCallback<Status>() {

                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    Log.i(TAG, "unpublish: unpublished successfully");

                                    if (mStack.size() != 0) {
                                        //If we still have messages to send we try to send it
                                        Log.i(TAG, "unpublish: return to sending");
                                        updateSharedPreference(Constants.KEY_PUBLICATION_TASK,
                                                Constants.TASK_PUBLISH);
                                    } else {
                                        Log.i(TAG, "unpublish: stack is empty");

                                        updateSharedPreference(Constants.KEY_PUBLICATION_TASK,
                                                Constants.TASK_NONE);
                                    }

                                } else {
                                    Log.i(TAG, "unpublish: could not unpublish");
                                    handleUnsuccessfulNearbyResult(status);
                                }
                            }
                        });
            }
        }
    }

    /**
     * Handles errors generated when performing a subscription or publication action. Uses
     * {@link Status#startResolutionForResult} to display an opt-in dialog to handle the case
     * where a device is not opted into using Nearby.
     */
    private void handleUnsuccessfulNearbyResult(Status status) {
        Log.i(TAG, "processing error, status = " + status);
        if (status.getStatusCode() == NearbyMessagesStatusCodes.APP_NOT_OPTED_IN) {
            if (!mResolvingNearbyPermissionError) {
                try {
                    mResolvingNearbyPermissionError = true;
                    status.startResolutionForResult(getActivity(),
                            Constants.REQUEST_RESOLVE_ERROR);

                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (status.getStatusCode() == ConnectionResult.NETWORK_ERROR) {
                Toast.makeText(getActivity().getApplicationContext(),
                        "No connectivity, cannot proceed. Fix in 'Settings' and try again.",
                        Toast.LENGTH_LONG).show();
            } else {
                // To keep things simple, pop a toast for all other error messages.
                Toast.makeText(getActivity().getApplicationContext(), "Unsuccessful: " +
                        status.getStatusMessage(), Toast.LENGTH_LONG).show();
            }

        }
    }

    void executePendingTasks() {
        executePendingSubscriptionTask();
        executePendingPublicationTask();
    }

    /**
     * Invokes a pending task based on the subscription state.
     */
    void executePendingSubscriptionTask() {
        String pendingSubscriptionTask = getPubSubTask(Constants.KEY_SUBSCRIPTION_TASK);
        if (TextUtils.equals(pendingSubscriptionTask, Constants.TASK_SUBSCRIBE)) {
            subscribe();
        } else if (TextUtils.equals(pendingSubscriptionTask, Constants.TASK_UNSUBSCRIBE)) {
            unsubscribe();
        }
    }

    /**
     * Invokes a pending task based on the publication state.
     */
    void executePendingPublicationTask() {
        String pendingPublicationTask = getPubSubTask(Constants.KEY_PUBLICATION_TASK);
        if (TextUtils.equals(pendingPublicationTask, Constants.TASK_PUBLISH)) {
            publish();
        } else if (TextUtils.equals(pendingPublicationTask, Constants.TASK_UNPUBLISH)) {
            unpublish();
        }
    }

    void resetToDefaultState() {
        getActivity().getPreferences(Context.MODE_PRIVATE)
                .edit()
                .putString(Constants.KEY_SUBSCRIPTION_TASK, Constants.TASK_NONE)
                .putString(Constants.KEY_PUBLICATION_TASK, Constants.TASK_NONE)
                .apply();
    }

    /**
     * Helper for editing entries in SharedPreferences.
     */
    private void updateSharedPreference(String key, String value) {
        getActivity().getPreferences(Context.MODE_PRIVATE)
                .edit()
                .putString(key, value)
                .apply();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, final String key) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (TextUtils.equals(key, Constants.KEY_SUBSCRIPTION_TASK)) {
                    executePendingSubscriptionTask();
                } else if (TextUtils.equals(key, Constants.KEY_PUBLICATION_TASK)) {
                    executePendingPublicationTask();
                }
            }
        });
    }
}