package com.blakgeek.cordova.plugin;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FirebaseAuthPlugin extends CordovaPlugin implements OnCompleteListener<AuthResult>, FirebaseAuth.AuthStateListener {

    private static final int RC_SIGN_IN = 9001;
    private GoogleApiClient googleApiClient;
    private CallbackContext eventContext;
    private FirebaseAuth firebaseAuth;
    private List<String> allowedDomains = new ArrayList<>();
    private String currentToken;

    @Override
    protected void pluginInitialize() {


        Context context = this.cordova.getActivity().getApplicationContext();
        String defaultClientId = getDefaultClientId(context);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(defaultClientId)
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

    private String getDefaultClientId(Context context) {

        String packageName = context.getPackageName();
        int id = context.getResources().getIdentifier("default_web_client_id", "string", packageName);
        return context.getString(id);
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

    private boolean  initialize(JSONArray args, CallbackContext callbackContext) {

        JSONArray allowedDomains = args.optJSONArray(0);
        this.allowedDomains = new ArrayList<>();

        if(allowedDomains != null) {
            for (int i=0; i < allowedDomains.length(); i++) {
                this.allowedDomains.add(allowedDomains.optString(i));
            }
        }

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

        String email = acct.getEmail();
        String domain = email.substring(email.indexOf('@') + 1);
        if(allowedDomains.size() == 0 || allowedDomains.contains(domain)) {
            AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
            firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this);
        } else {
            if(googleApiClient.isConnected()) {
                Auth.GoogleSignInApi.signOut(googleApiClient);
            }
            JSONObject error = new JSONObject();
            try {
                error.put("code", "domain_not_allowed");
                error.put("message", "the domain is not allowed");
            } catch (JSONException e) {
            }
            raiseEvent("signinfailure", error);
        }
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

        final FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            user.getToken(false).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                @Override
                public void onComplete(@NonNull Task<GetTokenResult> task) {

                    final JSONObject data = new JSONObject();
                    String token = task.getResult().getToken();

                    if(token != null && !token.equals(currentToken)) {
                        currentToken = token;
                        try {
                            data.put("token", token);
                            data.put("name", user.getDisplayName());
                            data.put("email", user.getEmail());
                            data.put("id", user.getUid());
                            if (user.getPhotoUrl() != null) {
                                data.put("photoUrl", user.getPhotoUrl().toString());
                            }
                        } catch (JSONException e) {
                        }
                        raiseEvent("signinsuccess", data);
                    }
                }
            });
        } else if(currentToken != null){
            raiseEvent("signoutsuccess");
            currentToken = null;
        }
    }
}


