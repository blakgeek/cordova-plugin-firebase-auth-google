package com.blakgeek.cordova.plugin;

// IMPORT_R
import android.content.Context;
import android.content.Intent
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

public class FirebaseAuthPlugin extends CordovaPlugin implements GoogleApiClient.OnConnectionFailedListener {

    private static final int RC_SIGN_IN = 9001;
    private GoogleApiClient googleApiClient;
    @Override
    protected void pluginInitialize() {

        Context context = this.cordova.getActivity().getApplicationContext();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleApiClient = new GoogleApiClient.Builder(context)
//                .enableAutoManage(this.cordova.getActivity(), this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        super.pluginInitialize();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch(action) {
            case "initialize":
                return initialize(args, callbackContext);
            case "signIn":
                return signIn(args, callbackContext);
            default:
                return false;
        }
    }

    private boolean signIn(JSONArray args, CallbackContext callbackContext) {

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        this.cordova.getActivity().startActivityForResult(signInIntent, RC_SIGN_IN);
        callbackContext.success();
        return true;
    }

    private boolean initialize(JSONArray args, CallbackContext callbackContext) {
        callbackContext.success();
        return true;
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
