#import "FirebaseAuthPlugin.h"
#import "AppDelegate.h"
#import "objc/runtime.h"
@import Firebase;

@implementation FirebaseAuthPlugin

- (void)initialize:(CDVInvokedUrlCommand *)command {
    
    if(![FIRApp defaultApp]) {
        [FIRApp configure];
    }
    [GIDSignIn sharedInstance].clientID = [FIRApp defaultApp].options.clientID;
    [GIDSignIn sharedInstance].uiDelegate = self.viewController;
    [GIDSignIn sharedInstance].delegate = self;

    self.eventCallbackId = command.callbackId;
}

- (void)signIn:(CDVInvokedUrlCommand *)command {

    [[GIDSignIn sharedInstance] signIn];
}

- (void)signOut:(CDVInvokedUrlCommand *)command {

    NSDictionary *message = nil;
    NSError *error;

    [[GIDSignIn sharedInstance] signOut];
    [[FIRAuth auth] signOut:&error];

    if (error == nil) {
        message = @{
                @"type" : @"signoutsuccess"
        };
    } else {

        message = @{
                @"type" : @"signoutfailure",
                @"data" : @{
                        @"code" : [NSNumber numberWithInteger:error.code],
                        @"message" : error.description == nil ? [NSNull null] : error.description
                }
        };
    }

    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:message];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.eventCallbackId];
}

#pragma mark - Helper functions

- (NSString *)toJSON:(NSDictionary *)data {
    NSError *error = nil;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:data options:NSJSONWritingPrettyPrinted error:&error];

    return [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
}

- (void)signIn:(GIDSignIn *)signIn didSignInForUser:(GIDGoogleUser *)user withError:(NSError *)error {

    NSDictionary *message = nil;
    if (error == nil) {
        GIDAuthentication *authentication = user.authentication;
        FIRAuthCredential *credential = [FIRGoogleAuthProvider credentialWithIDToken:authentication.idToken
                                                                         accessToken:authentication.accessToken];
        [[FIRAuth auth] signInWithCredential:credential
                                  completion:[self handleLogin]];
    } else {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:@{
                @"type" : @"signinfailure",
                @"data" : @{

                        @"code" : [NSNumber numberWithInteger:error.code],
                        @"message" : error.description
                }
        }];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.eventCallbackId];
    }

}

- (void (^)(FIRUser *, NSError *))handleLogin {
    return ^(FIRUser *user, NSError *error) {

        NSDictionary *message;
        if (error == nil) {

            message = @{
                    @"type" : @"signinsuccess",
                    @"data" : @{
                            @"id" : user.uid == nil ? [NSNull null] : user.uid,
                            @"name" : user.displayName == nil ? [NSNull null] : user.displayName,
                            @"email" : user.email == nil ? [NSNull null] : user.email,
                            @"photoUrl" : user.photoURL == nil ? [NSNull null] : [user.photoURL absoluteString]
                    }
            };
        } else {
            message = @{
                    @"type" : @"signinfailure",
                    @"data" : @{
                            @"code" : [NSNumber numberWithInteger:error.code],
                            @"message" : error.description == nil ? [NSNull null] : error.description
                    }
            };
        }

        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:message];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.eventCallbackId];
    };
}

- (void)signIn:(GIDSignIn *)signIn didDisconnectWithUser:(GIDGoogleUser *)user withError:(NSError *)error {

    NSDictionary *message = nil;
    if (error == nil) {
        GIDProfileData *profile = user.profile;
        message = @{
                @"type" : @"signoutsuccess"
        };
    } else {
        message = @{
                @"type" : @"signoutfailure",
                @"data" : @{

                        @"code" : [NSNumber numberWithInteger:error.code],
                        @"message" : error.description == nil ? [NSNull null] : error.description
                }
        };
    }

    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:message];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.eventCallbackId];
}

@end