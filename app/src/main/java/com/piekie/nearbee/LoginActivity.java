package com.piekie.nearbee;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.iid.InstanceID;
import com.piekie.nearbee.utils.ChatMessage;
import com.piekie.nearbee.utils.Constants;
import com.piekie.nearbee.utils.Profile;

public class LoginActivity extends AppCompatActivity {

    private EditText identifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        identifier = (EditText) findViewById(R.id.login_identifier);

        // Getting sharedPreferences instance of string
        // where we have saved data about last identifier
        String lastIdentifier = this.getPreferences(Context.MODE_PRIVATE)
                .getString(Constants.LAST_IDENTIFIER, Constants.NONE);
        if (!lastIdentifier.equals(Constants.NONE)) {
            // Setting text of identifier's EditText to last inputted identifier */
            identifier.setText(lastIdentifier);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        /** Check if this activity was launched before */
        if (getPreferences(Context.MODE_PRIVATE).getBoolean(Constants.WAS_LAUNCHED, false)) {
            // True: call onDestroy() and skip this activity
            //  Set "was_launched" flag to false for next launch.

            getPreferences(Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(Constants.WAS_LAUNCHED, false)
                    .apply();
            onDestroy();
        } else {
            // False: continue working. Set "was_launched" flag to true
            getPreferences(Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(Constants.WAS_LAUNCHED, true)
                    .apply();
        }
    }

    /** Handling onClickEvent and passing to next activity
     * @param v View for the onClick listener
     */
    public void useIdentifier(View v) {
        // Getting text from EditText
        String idText = identifier.getText().toString();

        if (idText.length() >= Constants.ID_MIN_SYMBOLS) {
            Profile profile = new Profile(InstanceID.getInstance(this.getApplicationContext()).getId(), idText);

            // Push idText to preferences for the next launch
            getPreferences(Context.MODE_PRIVATE)
                    .edit()
                    .putString(Constants.LAST_IDENTIFIER, idText)
                    .apply();

            // Starting of ChatActivity
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("Profile", ChatMessage.gson.toJson(profile));
            startActivity(intent);
        } else {
            // Message about length of identifier: must be >= ID_MIN_SYMBOLS
            Toast t = Toast.makeText(getApplicationContext(), Constants.TOO_SMALL_ID, Toast.LENGTH_SHORT);
            t.show();
        }
    }
}
