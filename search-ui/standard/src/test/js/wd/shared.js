/*jslint node: true */
/* global after, before, beforeEach, require */
'use strict';

require('colors');
var argv = require('yargs').argv;
var chai = require('chai');
var chaiAsPromised = require('chai-as-promised');
var wd = require('wd');

chai.use(chaiAsPromised);
chai.should();
chaiAsPromised.transferPromiseness = wd.transferPromiseness;

function debugLogging(browser) {
    // optional extra logging
    browser.on('status', function (info) {
        console.log(info.cyan);
    });
    browser.on('command', function (eventType, command, response) {
        console.log(' > ' + eventType.cyan, command, (response || '').grey);
    });
    browser.on('http', function (meth, path, data) {
        console.log(' > ' + meth.magenta, path, (data || '').grey);
    });
}

var url = argv.url || 'http://localhost:8383/';

exports.asserters = wd.asserters;

exports.setup = function(mocha) {
    mocha.timeout(this.mochaOptions ? this.mochaOptions.timeout : 30000);

    before(function () {
        if (argv.browser) {
            this.browser = wd.promiseChainRemote();
        }

        if (argv.verbose) {
            debugLogging(this.browser);
        }

        return this.browser
            .init({browserName: argv.browser || 'chrome'})
            .setAsyncScriptTimeout(10000)
            //set window size for phantomjs
            .setWindowSize(1200, 1200)
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
