package com.blakgeek.cordova.plugin;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
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

public class FirebaseAuthGooglePlugin extends CordovaPlugin implements OnCompleteListener<AuthResult>, FirebaseAuth.AuthStateListener, ResultCallback<GoogleSignInResult> {

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

        int googlePlayAvailabilityStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable( context);
        if(googlePlayAvailabilityStatus == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED){
            raiseEvent("googlePlayServiceUpdateRequired");
        }

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
            case "getToken":
                return getToken(callbackContext);
            case "signIn":
                return signIn(args.getBoolean(0));
            case "signOut":
                return signOut();
            default:
                return false;
        }
    }


    private boolean getToken(final CallbackContext callbackContext) {

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null) {
            user.getIdToken(false).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                @Override
                public void onComplete(@NonNull Task<GetTokenResult> task) {

                    callbackContext.success(task.getResult().getToken());
                }
            });
        } else if (currentToken != null) {
            callbackContext.error("no_user_found");
        }
        return true;
    }

    private boolean signOut() {
        Auth.GoogleSignInApi.signOut(googleApiClient);
        FirebaseAuth.getInstance().signOut();
        return true;
    }

    private boolean signIn(boolean silent) {

        if(silent) {
            OptionalPendingResult<GoogleSignInResult> result = Auth.GoogleSignInApi.silentSignIn(googleApiClient);
            if(result.isDone()) {
                handleSignInResult(result.get());
            } else {
                result.setResultCallback(this);
            }
        } else {
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
            this.cordova.startActivityForResult(this, signInIntent, RC_SIGN_IN);
        }
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
                error.put("message", "the domain [" + domain + "] is not allowed");
                error.put("domain", domain);
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
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {

            firebaseAuthWithGoogle(result.getSignInAccount());
        } else {
            JSONObject data = new JSONObject();
            try {
                Status status = result.getStatus();
                Object code = translateStatusCode(status.getStatusCode());
                data.put("code", code);
                data.put("message", result.getStatus().toString());
            } catch (JSONException e) {
            }
            raiseEvent("signinfailure", data);
        }
    }

    @Override
    public void onComplete(@NonNull Task<AuthResult> task) {

        if (!task.isSuccessful()) {
            Exception err = task.getException();
            JSONObject data = new JSONObject();
            try {
                data.put("code", err.getClass().getSimpleName());
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
            user.getIdToken(false).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                @Override
                public void onComplete(@NonNull Task<GetTokenResult> task) {

                    final JSONObject data = new JSONObject();

                    if(task.isSuccessful()) {

                        String token = task.getResult().getToken();

                        if (token != null && !token.equals(currentToken)) {
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
                    } else {

                        Exception err = task.getException();
                        try {
                            data.put("code", err.getClass().getSimpleName());
                            data.put("message", err.getMessage());
                        } catch (JSONException e) {
                        }
                        raiseEvent("signinfailure", data);
                    }
                }
            });
        } else if(currentToken != null){
            raiseEvent("signoutsuccess");
            currentToken = null;
        }
    }

    @Override
    public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
        handleSignInResult(googleSignInResult);
    }

    private Object translateStatusCode(int statusCode) {
        switch(statusCode) {
            case -1:
                return "SUCCESS_CACHE";
            case 0:
                return "SUCCESS";
            case 2:
                return "SERVICE_VERSION_UPDATE_REQUIRED";
            case 3:
                return "SERVICE_DISABLED";
            case 4:
                return "SIGN_IN_REQUIRED";
            case 5:
                return "INVALID_ACCOUNT";
            case 6:
                return "RESOLUTION_REQUIRED";
            case 7:
                return "NETWORK_ERROR";
            case 8:
                return "INTERNAL_ERROR";
            case 10:
                return "DEVELOPER_ERROR";
            case 13:
                return "ERROR";
            case 14:
                return "INTERRUPTED";
            case 15:
                return "TIMEOUT";
            case 16:
                return "CANCELED";
            case 17:
                return "API_NOT_CONNECTED";
            case 18:
                return "DEAD_CLIENT";
            case 12500:
                return "SIGN_IN_FAILED";
            case 12501:
                return "SIGN_IN_CANCELLED";
            default:
                return statusCode;
        }
    }

}


