var exec = require('cordova/exec');

function FirebaseAuth() {

    exec(dispatchEvent, null, 'FirebaseAuthPlugin', 'initialize', [])

    this.signIn = function () {

        return exec(null, null, 'FirebaseAuthPlugin', 'signIn', []);
    };

    function dispatchEvent(event) {

        window.dispatchEvent(new CustomEvent(event.type, {detail: event.data}));
    }
}

if (typeof module !== undefined && module.exports) {

    module.exports = FirebaseAuth;
}