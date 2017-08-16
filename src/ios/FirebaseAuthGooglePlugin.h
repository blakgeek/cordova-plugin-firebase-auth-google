#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import <GoogleSignIn/GoogleSignIn.h>

@interface FirebaseAuthGooglePlugin : CDVPlugin <GIDSignInDelegate, GIDSignInUIDelegate>

- (void)initialize:(CDVInvokedUrlCommand *)command;
- (void)signIn:(CDVInvokedUrlCommand *)command;
- (void)signOut:(CDVInvokedUrlCommand *)command;
- (void)getToken:(CDVInvokedUrlCommand *)command;
@property (nonatomic) NSString *eventCallbackId;
@property (strong, nonatomic) NSArray *allowedDomains;
@end
