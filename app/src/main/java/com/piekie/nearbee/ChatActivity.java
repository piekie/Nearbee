package com.piekie.nearbee;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.piekie.nearbee.utils.ChatMessage;
import com.piekie.nearbee.utils.Constants;
import com.piekie.nearbee.utils.Profile;

public class ChatActivity extends AppCompatActivity {

    /**
     * Tag of activity (for logging)
     */
    private static final String MAIN_FRAGMENT_TAG = "main_fragment_tag";

    /**
     * Static instance of Profile class to save all personal data
     * and with ability to be accessed from all the parts of app
     */
    public static Profile myProfile;

    private NearbyFragment mMainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Getting profile instance from Intent (LoginActivity)
        myProfile = ChatMessage.gson.fromJson(getIntent().getStringExtra("Profile"), Profile.class);

        FragmentManager fm = getFragmentManager();
        mMainFragment = (NearbyFragment) fm.findFragmentByTag(MAIN_FRAGMENT_TAG);

        //If we can't access to fragment (it did not created) we try to create it
        if (mMainFragment == null) {
            mMainFragment = new NearbyFragment();
            fm.beginTransaction().add(R.id.container, mMainFragment, MAIN_FRAGMENT_TAG).commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mMainFragment.finishedResolvingNearbyPermissionError();
        if (requestCode == Constants.REQUEST_RESOLVE_ERROR) {
            // User was presented with the Nearby opt-in dialog and pressed "Allow".
            if (resultCode == Activity.RESULT_OK) {
                // We track the pending subscription and publication tasks in MainFragment. Once
                // a user gives consent to use Nearby, we execute those tasks.
                mMainFragment.executePendingTasks();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // User was presented with the Nearby opt-in dialog and pressed "Deny". We cannot
                // proceed with any pending subscription and publication tasks. Reset state.
                 mMainFragment.resetToDefaultState();
            } else {
                Toast.makeText(this, "Failed to resolve error with code " + resultCode,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
