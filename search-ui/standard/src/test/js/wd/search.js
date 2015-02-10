/*jslint node: true */
/* global describe, it, require */
'use strict';

var shared = require('./shared');

var asserters = shared.asserters;

describe('Contextual', function () {
    shared.setup(this);

    describe('wildcard query', function () {

        it("should show the map", function () {
            return this.browser
                .waitForElementByCssSelector('canvas.ol-unselectable', 20000);
        });

        it("should allow saving searches", function () {
            return this.browser
                .waitForElementByCssSelector('form#searchForm input[name="q"]', 20000)
                // getLocationInView does not work in Firefox and IE
                .safeExecute('document.querySelectorAll("#searchButton")[0].scrollIntoView(true)')
                .elementById('saveButton').click()
                .waitForElementByCssSelector('form#workspaceSelectForm input[name="searchName"]', 20000)
                .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('search-save.png'))
                .elementById('cancel').click();
        });

        it("should allow search", function () {
            return this.browser
                .waitForElementByCssSelector('form#searchForm input[name="q"]', 20000)
                .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('search-form.png'))
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
                .waitForConditionInBrowser('document.querySelectorAll("a.metacard-link").length >= 10', 10000)
                .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('results-list.png'));
        });

        it("should display source status and filter options", function () {
            return this.browser
                .waitForElementById('status-icon', 10000).click()
                .waitForElementById('status-table', asserters.isDisplayed, 10000)
                .waitForElementByCssSelector('.filter-view.active', asserters.isDisplayed, 10000)
                .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('results-filters.png'));
        });

        it("should be able to display metacard details", function () {
            return this.browser
                .waitForElementByCssSelector('a.metacard-link').click()
                .waitForElementByClassName('metacard-details', asserters.nonEmptyText)
                .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('record-summary.png'));
        });

        it("should be able to display metacard details", function () {
            return this.browser
                .waitForElementByLinkText('Details').click()
                .waitForElementByClassName('metacard-table', asserters.isDisplayed)
                .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('record-details.png'));
        });

        it("should be able to display metacard actions", function () {
            return this.browser
                .waitForElementByLinkText('Actions').click()
                .waitForElementByCssSelector('#actions.active', asserters.isDisplayed)
                .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('record-actions.png'));
        });

        it("should allow previous and next navigation", function () {
            return this.browser
                .waitForElementByCssSelector('#prevRecord.disabled')
                .waitForElementById('nextRecord', asserters.isDisplayed).click()
                .waitForElementByCssSelector('#prevRecord:not(.disabled)');
        });
    });
});
