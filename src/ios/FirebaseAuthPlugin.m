#import "FirebaseAuthPlugin.h"
#import "AppDelegate.h"
#import "objc/runtime.h"
@import Firebase;

static void swizzleMethod(Class class, SEL destinationSelector, SEL sourceSelector);

@implementation FirebaseAuthPlugin

- (void)pluginInitialize {

    swizzleMethod([AppDelegate class],
            @selector(application:openURL:options:),
            @selector(identity_application:openURL:options:));
    swizzleMethod([AppDelegate class],
            @selector(application:openURL:sourceApplication:annotation:),
            @selector(identity_application:openURL:sourceApplication:annotation:));

    [super pluginInitialize];
}

- (void)initialize:(CDVInvokedUrlCommand *)command {

    [FIRApp configure];
    [GIDSignIn sharedInstance].clientID = [FIRApp defaultApp].options.clientID;
    [GIDSignIn sharedInstance].uiDelegate = self.viewController;
    [GIDSignIn sharedInstance].delegate = self;

    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)signIn:(CDVInvokedUrlCommand *)command {

    [[GIDSignIn sharedInstance] signIn];
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

#pragma mark - Helper functions

- (NSString *)toJSON:(NSDictionary *)data {
    NSError *error = nil;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:data options:NSJSONWritingPrettyPrinted error:&error];

    return [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
}

- (void)signIn:(GIDSignIn *)signIn didSignInForUser:(GIDGoogleUser *)user withError:(NSError *)error {

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