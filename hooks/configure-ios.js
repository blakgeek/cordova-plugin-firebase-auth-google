var fs = require('fs');
var crypto = require('crypto');
var xcode = require('xcode');
var plist = require('plist');
var shell = require('shelljs');
var path = require('path');


module.exports = function (context) {

    //return;

    // only execute for android
    if (!forPlatform('ios')) return;

    var Q = context.requireCordovaModule('q');
    var deferred = Q.defer();
    var rootPath = context.opts.projectRoot;
    var appName = getConfigParser(context, 'config.xml').name();
    var iosConfigSrc = path.join(rootPath, 'GoogleService-Info.plist');
    var iosConfigDestDir = path.join(rootPath, 'platforms', 'ios', appName, '/Resources');
    var appPlistPath = path.join(rootPath,  'platforms', 'ios', appName, appName + '-Info.plist');
    var iosConfigDest = path.join(iosConfigDestDir, 'GoogleService-Info.plist');
    var projectPath = path.join(rootPath,  'platforms', 'ios', appName + '.xcodeproj/project.pbxproj');
    var iosConfigExists = fs.existsSync(iosConfigSrc);
    var project = xcode.project(projectPath);

    if (iosConfigExists && filesDiffer(iosConfigSrc, iosConfigDest)) {

        console.log('Copying GoogleService-Info.plist');
        shell.mkdir('-p', iosConfigDestDir);
        shell.cp(iosConfigSrc, iosConfigDest);

        var configPlist = plist.parse(fs.readFileSync(iosConfigSrc, 'utf8'));
        var appPlist = plist.parse(fs.readFileSync(appPlistPath, 'utf8'));
        appPlist.CFBundleURLTypes = (appPlist.CFBundleURLTypes || []).concat([
            {
                "CFBundleTypeRole": "Editor",
                "CFBundleURLSchemes": [configPlist.REVERSED_CLIENT_ID]
            }
        ]);
        fs.writeFileSync(appPlistPath, plist.build(appPlist));

        project.parse(function (err) {

            console.log('adding resource');
            project.addResourceFile('GoogleService-Info.plist');
            fs.writeFileSync(projectPath, project.writeSync());
            deferred.resolve();
        });
    } else {
        deferred.resolve();
    }

    return deferred.promise;


    function forPlatform(platform) {

        return context.opts.platforms.indexOf(platform) > -1;
    }

    function filesDiffer(a, b) {

        var aExists = fs.existsSync(a);
        var bExists = fs.existsSync(b);

        return !aExists || !bExists || hash(a) !== hash(b);
    }

    function hash(file) {

        var content = fs.readFileSync(file, 'utf8');
        return crypto.createHash('md5').update(content).digest('hex')
    }

    function getConfigParser(context, config) {
        var semver = context.requireCordovaModule('semver');
        var ConfigParser;

        if (semver.lt(context.opts.cordova.version, '5.4.0')) {
            ConfigParser = context.requireCordovaModule('cordova-lib/src/ConfigParser/ConfigParser');
        } else {
            ConfigParser = context.requireCordovaModule('cordova-common/src/ConfigParser/ConfigParser');
        }

        return new ConfigParser(config);
    }

};
