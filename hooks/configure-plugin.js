var fs = require('fs');
var path = require('path');

module.exports = function (context) {

    var appPackage = getConfigParser(context, 'config.xml').packageName();
    var rootPath = context.opts.projectRoot;
    var pluginJavaPath = path.join(rootPath, 'platforms', 'android', 'src', 'com', 'blakgeek', 'cordova', 'plugin', 'FirebaseAuthPlugin.java');
    var content = fs.readFileSync(pluginJavaPath, 'utf8');
    var updatedContent = content.replace(/\/\/ IMPORT_R/g, 'import ' + appPackage + '.R;');

    fs.writeFileSync(pluginJavaPath, updatedContent);

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
