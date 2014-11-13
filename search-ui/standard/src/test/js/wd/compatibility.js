/* global after, before, beforeEach, describe, it, require */

require('colors');
var argv = require('yargs').argv;
var chai = require('chai');
var chaiAsPromised = require('chai-as-promised');
var wd = this.wd || require('wd');

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

describe('Contextual search', function () {
    this.timeout(this.mochaOptions ? this.mochaOptions.timeout : 30000);
    var browser;

    before(function () {
        if (!argv.browser) {
            browser = this.browser;
        } else {
            browser = wd.promiseChainRemote();
        }

        if (argv.debug) {
            debugLogging(browser);
        }

        return browser
            .init({browserName: argv.browser || 'chrome'})
            .setAsyncScriptTimeout(10000)
            //set window size for phantomjs
            .setWindowSize(1200, 900);
    });

    beforeEach(function () {
        return browser.get(argv.url || 'http://localhost:8383/');
    });

    after(function () {
        if (argv.browser) {
            return browser.quit();
        } else {
            return browser;
        }
    });

    it("should return results on wildcard query", function () {
        return browser
            .waitForElementByCssSelector('form#searchForm input[name=q]')
            .elementByCssSelector('input[name="q"]').type('*')
            .elementById('searchButton').click()
            .waitForElementByClassName('result-count')
            .then(function (count) {
                count.text().should.eventually.contain('results displayed');
            })
            .waitForElementsByCssSelector('a.metacard-link', 10000)
            .waitForConditionInBrowser('document.querySelectorAll("a.metacard-link").length >= 10', 10000);
    });
});
