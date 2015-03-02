/*jslint node: true */
/* global after, before, beforeEach, require */
'use strict';

require('colors');
var argv = require('yargs').argv;
var chai = require('chai');
var chaiAsPromised = require('chai-as-promised');
var wd = require('wd');
var fs = require('fs');
var path = require('path');

chai.use(chaiAsPromised);
chai.should();
chaiAsPromised.transferPromiseness = wd.transferPromiseness;

function debugLogging(browser) {
    // optional extra logging
    if (browser.on) {
        browser.on('status', function (info) {
            console.log(info.cyan);
        });
        browser.on('command', function (eventType, command, response) {
            if (command === "takeScreenshot()") {
                response = "";
            }
            console.log(' > ' + eventType.cyan, command, (response || '').grey);
        });
        browser.on('http', function (meth, path, data) {
            console.log(' > ' + meth.magenta, path, (data || '').grey);
        });
    }
}

var screenshotPath = path.join(__dirname, '../../../..', 'target/webapp/images');
fs.mkdir(screenshotPath);

exports.screenshotPath = screenshotPath;

exports.getPathForScreenshot = function (filename) {
    return path.join(screenshotPath, filename);
};

var url = argv.url || 'http://localhost:8888?map=2d';

exports.asserters = wd.asserters;

exports.setup = function() {

    before(function () {
        exports.timeout = argv.timeout || this.mochaOptions.timeout || 30000;

        if (argv.browser) {
            this.browser = wd.promiseChainRemote()
                .init({browserName: argv.browser || 'chrome'});
        }

        if (argv.verbose) {
            debugLogging(this.browser);
        }

        return this.browser
            .setAsyncScriptTimeout(exports.timeout)
            .setWindowSize(1080, 1080)
            .get(url);
    });

    after(function () {
        if (argv.browser) {
            return this.browser.quit();
        } else {
            return this.browser;
        }
    });
};

exports.reloadBeforeTests = function() {
    before('refresh page', function () {
        return this.browser.get(url);
    });
};

exports.reloadBeforeEachTest = function() {
    beforeEach('refresh page', function () {
        return this.browser.get(url);
    });
};
