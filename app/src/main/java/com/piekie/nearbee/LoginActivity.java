package com.piekie.nearbee;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

        //TODO: Remembering of previous identifier
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
            finish();
        } else {
            // Message about length of identifier: must be >= ID_MIN_SYMBOLS
            Toast t = Toast.makeText(getApplicationContext(), Constants.TOO_SMALL_ID, Toast.LENGTH_SHORT);
            t.show();
        }
    }
}
