package com.blakgeek.cordova.plugin;

// IMPORT_R
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

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

public class FirebaseAuthPlugin extends CordovaPlugin implements OnCompleteListener<AuthResult>, FirebaseAuth.AuthStateListener {

    private static final int RC_SIGN_IN = 9001;
    private GoogleApiClient googleApiClient;
    private CallbackContext eventContext;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void pluginInitialize() {

        Context context = this.cordova.getActivity().getApplicationContext();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();

        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        googleApiClient.connect();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.addAuthStateListener(this);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "initialize":
                return initialize(args, callbackContext);
            case "signIn":
                return signIn();
            case "signOut":
                return signOut();
            default:
                return false;
        }
    }

    private boolean signOut() {
        Auth.GoogleSignInApi.signOut(googleApiClient);
        FirebaseAuth.getInstance().signOut();
        return true;
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

    private void raiseEvent(String type) {
        raiseEvent(type, null);
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

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this);
    }

    @Override
    public void onStart() {
        if (!googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
            googleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        if (googleApiClient.isConnected() || googleApiClient.isConnecting()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(intent);
            if (result.isSuccess()) {

                firebaseAuthWithGoogle(result.getSignInAccount());
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

    @Override
    public void onComplete(@NonNull Task<AuthResult> task) {

        if (!task.isSuccessful()) {
            Exception err = task.getException();
            JSONObject data = new JSONObject();
            try {
                data.put("code", "UH_OH");
                data.put("message", err.getMessage());
            } catch (JSONException e) {
            }
            raiseEvent("signinfailure", data);
        }
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            JSONObject data = new JSONObject();
            try {
                data.put("name", user.getDisplayName());
                data.put("email", user.getEmail());
                data.put("id", user.getUid());
                if (user.getPhotoUrl() != null) {
                    data.put("photoUrl", user.getPhotoUrl().toString());
                }
            } catch (JSONException e) {
            }
            raiseEvent("signinsuccess", data);
        } else {
            raiseEvent("signoutsuccess");
        }
    }
}


