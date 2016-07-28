#import "FirebaseAuthPlugin.h"
#import "AppDelegate.h"
#import "objc/runtime.h"
@import Firebase;

static void swizzleMethod(Class class, SEL destinationSelector, SEL sourceSelector);

@implementation AppDelegate (FoSwizzle)

+ (void)load {

    swizzleMethod([AppDelegate class],
            @selector(application:openURL:options:),
            @selector(identity_application:openURL:options:));
    swizzleMethod([AppDelegate class],
            @selector(application:openURL:sourceApplication:annotation:),
            @selector(identity_application:openURL:sourceApplication:annotation:));
}

#pragma mark - AppDelegate Swizzles

- (BOOL)identity_application:(UIApplication *)app
                     openURL:(NSURL *)url
                     options:(NSDictionary<NSString *, id> *)options {
    return [[GIDSignIn sharedInstance] handleURL:url
                               sourceApplication:options[UIApplicationOpenURLOptionsSourceApplicationKey]
                                      annotation:options[UIApplicationOpenURLOptionsAnnotationKey]];
}

- (BOOL)identity_application:(UIApplication *)application
                     openURL:(NSURL *)url
           sourceApplication:(NSString *)sourceApplication
                  annotation:(id)annotation {
    return [[GIDSignIn sharedInstance] handleURL:url
                               sourceApplication:sourceApplication
                                      annotation:annotation];
}
@end

@implementation FirebaseAuthPlugin

- (void)initialize:(CDVInvokedUrlCommand *)command {

    [FIRApp configure];
    [GIDSignIn sharedInstance].clientID = [FIRApp defaultApp].options.clientID;
    [GIDSignIn sharedInstance].uiDelegate = self.viewController;
    [GIDSignIn sharedInstance].delegate = self;

    self.eventCallbackId = command.callbackId;
}

- (void)signIn:(CDVInvokedUrlCommand *)command {

    [[GIDSignIn sharedInstance] signIn];
}

- (void)signOut:(CDVInvokedUrlCommand *)command {

    [[GIDSignIn sharedInstance] signOut];
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
                @"type" : @"signoutfailure",
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

static void swizzleMethod(Class class, SEL destinationSelector, SEL sourceSelector) {
    Method destinationMethod = class_getInstanceMethod(class, destinationSelector);
    Method sourceMethod = class_getInstanceMethod(class, sourceSelector);

    // If the method doesn't exist, add it.  If it does exist, replace it with the given implementation.
    if (class_addMethod(class, destinationSelector, method_getImplementation(sourceMethod), method_getTypeEncoding(sourceMethod))) {
        class_replaceMethod(class, destinationSelector, method_getImplementation(destinationMethod), method_getTypeEncoding(destinationMethod));
    } else {
        method_exchangeImplementations(destinationMethod, sourceMethod);
    }
}