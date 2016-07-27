

function FirebaseAuth() {

	this.init = function(successCallback, failureCallback) {

		return cordova.exec(successCallback, failureCallback, 'FirebaseAuthPlugin', 'initialize', []);
	};

	this.login = function(successCallback, failureCallback) {

		return cordova.exec(successCallback, failureCallback, 'FirebaseAuthPlugin', 'signIn', []);
	};

}

if(typeof module !== undefined && module.exports) {

	module.exports = FirebaseAuth;
}

