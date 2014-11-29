/*jslint node: true */
/* global describe, it, require */
'use strict';

var shared = require('./shared');
var asserters = shared.asserters;

describe('Contextual', function () {
    shared.setup(this);

    describe('wildcard query', function () {
        it("should allow search", function () {
            return this.browser
                .waitForElementByCssSelector('form#searchForm input[name="q"]', 20000)
                .elementByCssSelector('input[name="q"]').type('*')
                // getLocationInView does not work in Firefox and IE
                .safeExecute('document.querySelectorAll("#searchButton")[0].scrollIntoView(true)')
                .elementById('searchButton').click();
        });

        it("should show result count", function () {
            return this.browser
                .waitForElementByCssSelector('.result-count i', asserters.textInclude('results'), 10000);
        });

        it("should display results", function () {
            return this.browser
                .waitForElementsByCssSelector('a.metacard-link', 10000)
                .waitForConditionInBrowser('document.querySelectorAll("a.metacard-link").length >= 10', 10000);
        });

        it("should be able to display metacard details", function () {
            return this.browser
                .waitForElementByCssSelector('a.metacard-link').click()
                .waitForElementByClassName('metacard-details', asserters.nonEmptyText);
        });

        it("should allow previous and next navigation", function () {
            return this.browser
                .waitForElementByCssSelector('#prevRecord.disabled')
                .waitForElementById('nextRecord', asserters.isDisplayed).click()
                .waitForElementByCssSelector('#prevRecord:not(.disabled)');
        });

        it("should be able to display metacard details", function () {
            return this.browser
                .waitForElementByLinkText('Details').click()
                .waitForElementByClassName('metacard-table', asserters.isDisplayed);
        });
    });
});
