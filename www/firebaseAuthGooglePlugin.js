var exec = require('cordova/exec');
const PLUGIN_NAME = 'FirebaseAuthGooglePlugin';

function FirebaseAuth(options) {

    var self = this;
    options = options || {};
    var allowDomains = options.allowDomains ? [].concat(options.allowDomains) : null;
    exec(dispatchEvent, null, PLUGIN_NAME, 'initialize', [allowDomains]);

    this.getToken = function(success, failure) {

        if(window.Promise) {
            return new Promise(function (resolve, reject) {

                exec(resolve, reject, PLUGIN_NAME, 'getToken', []);
            });
        } else {
            return exec(success, failure, PLUGIN_NAME, 'getToken', []);
        }
    };

    this.signIn = function (silent) {

        return exec(null, null, PLUGIN_NAME, 'signIn', [silent === true]);
    };

    this.signOut = function () {

        return exec(null, null, PLUGIN_NAME, 'signOut', []);
    };

    function dispatchEvent(event) {

        // dispatch a pre-sign event that can be cancelled
        if(event.type === 'signinsuccess') {

            var cancelled = !window.dispatchEvent(new CustomEvent('beforeSignInComplete', {
                detail: event.data,
                cancelable: true
            }));

            if(cancelled) {
                self.signOut();
            } else {
                window.dispatchEvent(new CustomEvent(event.type, {
                    detail: event.data,
                    cancelable: true
                }));
            }
        } else {
            window.dispatchEvent(new CustomEvent(event.type, {
                detail: event.data,
                cancelable: true
            }));
        }
    }
}

if (typeof module !== undefined && module.exports) {

    module.exports = FirebaseAuth;
}