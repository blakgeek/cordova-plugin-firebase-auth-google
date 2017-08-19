# cordova-plugin-firebase-auth-google
A super awesome plugin that let's you login into you app Firebase and a Google account.
 
## How do I install it?

If you're like me and using [Cordova CLI](http://cordova.apache.org/):
```
cordova plugin add cordova-plugin-firebase-auth-google
```

or

```
phonegap local plugin add cordova-plugin-app-name --variable APP_NAME="Look <Ma> I Got Spaces and Stuff!"
```

## How do I use it?
1. Download the google-services.json and/or GoogleService-Info.plist for you application from the Firebase console.
1. Copy the files from previous step to root of your project.  If you don't do this plugin's hook won't be able to copy 
them to the appropriate locations and bad things will happen at runtime.  So make you do this.
1. Now you're ready to write some code.

```javascript
const fbAuth = new FirebaseAuth({
    allowDomains: ['blakgeek.com'] // optionally restrict the domains that can used to log into the app.
})
````

## How do I sign in
```javascript
// on succesful sign in this event will be fired on the window (fancy name huh?)
window.addEventListener('signinsuccess', function (event) {

    console.log('Wassup?');
    // the detail property of the event will contain the information about the user 
    // ("token", "id", "name", "email", "id",  "photoUrl")
    console.dir(event.detail);
}, false);

// on an error during sign in this even will raised
window.addEventListener('signinfailure', function (event) {

    console.error('Oh snap');
    // the detail property will contain the error details 
    // (code, message and optionally the domain)
    // domain is included if the domain of user's email was not in the allowDomain list
    console.error(event.detail)
}, false);

// sign in by showing a UI
fbAuth.signIn();

// attempt to sign in without a UI (silently)
fbAuth.signIn(true); 
```

## How do I sign out?
```javascript
window.addEventListener('signoutsuccess', function (event) {

    console.log('Holla!')
}, false);
fbAuth.signOut();
```

## How do I get the user's token?
```javascript
fbAuth.getToken().then(token => {
    console.log(token);
}).catch(err => {
    console.error(err);
});
// or old school 
fbAuth.getToken(token => {
    console.log(token);
}, err => {
    console.error(err);
});
```




