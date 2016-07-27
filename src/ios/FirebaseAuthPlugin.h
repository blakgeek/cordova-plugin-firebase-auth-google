#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import <GoogleSignIn/GoogleSignIn.h>

@interface FirebaseAuthPlugin : CDVPlugin <GIDSignInDelegate, GIDSignInUIDelegate>
- (void)initialize:(CDVInvokedUrlCommand *)command;
- (void)signIn:(CDVInvokedUrlCommand *)command;
@end
