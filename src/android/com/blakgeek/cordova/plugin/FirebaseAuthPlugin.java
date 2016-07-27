package com.blakgeek.cordova.plugin;

// IMPORT_R
import android.content.Context;
import android.content.Intent;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FirebaseAuthPlugin extends CordovaPlugin {

    private static final int RC_SIGN_IN = 9001;
    private GoogleApiClient googleApiClient;
    private CallbackContext eventContext;

    @Override
    protected void pluginInitialize() {

        Context context = this.cordova.getActivity().getApplicationContext();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "initialize":
                return initialize(args, callbackContext);
            case "signIn":
                return signIn();
            default:
                return false;
        }
    }

    private boolean signIn() {

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        this.cordova.startActivityForResult(this, signInIntent, RC_SIGN_IN);
        return true;
    }

    private boolean initialize(JSONArray args, CallbackContext callbackContext) {
        if (eventContext == null) {
            eventContext = callbackContext;
        }
        return true;
    }

    private void raiseEvent(String type, Object data) {

        if (eventContext != null) {

            JSONObject event = new JSONObject();
            try {
                event.put("type", type);
                event.put("data", data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            PluginResult result = new PluginResult(PluginResult.Status.OK, event);
            result.setKeepCallback(true);
            eventContext.sendPluginResult(result);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(intent);
            if (result.isSuccess()) {

                GoogleSignInAccount account = result.getSignInAccount();
                JSONObject data = new JSONObject();
                if (account != null) {
                    try {
                        data.put("name", account.getDisplayName());
                        data.put("email", account.getEmail());
                        data.put("id", account.getId());
                        if (account.getPhotoUrl() != null) {
                            data.put("photoUrl", account.getPhotoUrl().toString());
                        }
                    } catch (JSONException e) {
                    }
                }
                raiseEvent("signinsuccess", data);

            } else {
                JSONObject data = new JSONObject();
                try {
                    data.put("code", result.getStatus().getStatusCode());
                    data.put("message", result.getStatus().getStatusMessage());
                } catch (JSONException e) {
                }
                raiseEvent("signinfailure", data);
            }
        }
    }
}


