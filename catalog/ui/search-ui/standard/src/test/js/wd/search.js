/*jslint node: true, loopfunc:true */
/* global describe, it, require */
'use strict';

var shared = require('./shared');

var asserters = shared.asserters;

for (var i = 0; i < shared.iterations; i++) {
    describe('Contextual:', function () {
        shared.setup(this);
        describe('wildcard query:', function () {
            it("should show the map", function () {
                return this.browser
                    .waitForElementByCssSelector('canvas.ol-unselectable', shared.timeout);
            });

            it("should allow saving searches", function () {
                return this.browser
                    .waitForElementByCssSelector('form#searchForm input[name="q"]', shared.timeout)
                    // getLocationInView does not work in Firefox and IE
                    .safeExecute('document.querySelectorAll("#searchButton")[0].scrollIntoView(true)')
                    .elementById('saveButton').click()
                    .waitForElementByCssSelector('form#workspaceSelectForm input[name="searchName"]', shared.timeout)
                    .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('search-save.png'))
                    .elementById('cancel').click();
            });

            it("should allow search", function () {
                return this.browser
                    .waitForElementByCssSelector('form#searchForm input[name="q"]', shared.timeout)
                    .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('search-form.png'))
                    .elementByCssSelector('input[name="q"]').type('*')
                    // getLocationInView does not work in Firefox and IE
                    .safeExecute('document.querySelectorAll("#searchButton")[0].scrollIntoView(true)')
                    .elementById('searchButton').click();
            });

            it("should display results", function () {
                return this.browser
                    .waitForElementsByCssSelector('div#progressRegion:empty', shared.timeout)
                    .waitForElementByCssSelector('.result-count i', asserters.textInclude('results'), shared.timeout)
                    .waitForElementsByCssSelector('a.metacard-link', shared.timeout)
                    .waitForConditionInBrowser('document.querySelectorAll("a.metacard-link").length >= 10', shared.timeout)
                    .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('results-list.png'));
            });

            it("should return different results after editing search keyword", function () {
                return this.browser
                    .waitForElementByClassName('backNavText').click()
                    .waitForConditionInBrowser("document.querySelectorAll('#searchButton').length > 0")
                    .waitForConditionInBrowser('document.querySelector("#searchButton").getBoundingClientRect().left > 0')
                    .waitForElementByCssSelector('form#searchForm input[name="q"]', shared.timeout)
                    .elementByCssSelector('input[name="q"]').clear().type('notfound')
                    .safeExecute('document.querySelectorAll("#searchButton")[0].scrollIntoView(true)')
                    .elementById('searchButton').click()
                    .waitForElementsByCssSelector('div#progressRegion:empty', shared.timeout)
                    .waitForElementByCssSelector('.result-count i', asserters.textInclude('results'), shared.timeout)
                    .waitForConditionInBrowser('document.querySelectorAll("a.metacard-link").length === 0', shared.timeout)
                    .waitForElementByClassName('backNavText').click()
                    .waitForConditionInBrowser("document.querySelectorAll('#searchButton').length > 0")
                    .waitForConditionInBrowser('document.querySelector("#searchButton").getBoundingClientRect().left > 0')
                    .waitForElementByCssSelector('form#searchForm input[name="q"]', shared.timeout)
                    .elementByCssSelector('input[name="q"]').clear().type('*')
                    .safeExecute('document.querySelectorAll("#searchButton")[0].scrollIntoView(true)')
                    .elementById('searchButton').click()
                    .waitForElementsByCssSelector('div#progressRegion:empty', shared.timeout)
                    .waitForElementByCssSelector('.result-count i', asserters.textInclude('results'), shared.timeout)
                    .waitForElementsByCssSelector('a.metacard-link', shared.timeout)
                    .waitForConditionInBrowser('document.querySelectorAll("a.metacard-link").length >= 10', shared.timeout);
            });

            it("should display source status and filter options", function () {
                return this.browser
                    .waitForElementById('status-icon', shared.timeout).click()
                    .waitForElementById('status-table', asserters.isDisplayed, shared.timeout)
                    .waitForElementByCssSelector('.filter-view.active', asserters.isDisplayed, shared.timeout)
                    .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('results-filters.png'));
            });

            it("should allow for filtered search", function () {
                return this.browser
                    .waitForElementByCssSelector('div.value-input-group input[name="stringValue1"]', shared.timeout)
                    .elementByCssSelector('input[name="stringValue1"]').clear().type('notfound')
                    .elementByCssSelector('.apply').click()
                    .waitForConditionInBrowser('document.querySelector(".result-count i").innerText.indexOf("0 ") === 0', shared.timeout)
                    .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('result-notfound.png'))
                    .elementByCssSelector('input[name="stringValue1"]').clear().type('*')
                    .elementByCssSelector('.apply').click()
                    .waitForConditionInBrowser('document.querySelectorAll("a.metacard-link").length >= 10', shared.timeout);
            });

            it("should be able to display uncached metacard summary", function () {
                return this.browser
                    .waitForElementByCssSelector('a.metacard-link').click()
                    .waitForElementByClassName('metacard-details', asserters.nonEmptyText)
                    .elementByCssSelector('#summary .pull-right .attribute-value', asserters.textInclude('Unknown'), shared.timeout)
                    .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('record-summary.png'));
            });

            it("should be able to display cached metacard summary", function () {
                return this.browser
                    .waitForElementById('nextRecord', asserters.isDisplayed, shared.timeout).click()
                    .waitForConditionInBrowser('document.querySelector("#summary .pull-right .attribute-value").innerHTML.indexOf("ago") >= 0', shared.timeout)
                    .waitForElementById('prevRecord', asserters.isDisplayed, shared.timeout).click()
                    .waitForConditionInBrowser('document.querySelector("#summary .pull-right .attribute-value").innerHTML.indexOf("Unknown") >= 0', shared.timeout);
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
                    .waitForElementById('nextRecord', asserters.isDisplayed, shared.timeout).click()
                    .waitForElementByCssSelector('#prevRecord:not(.disabled)');
            });
        });
    });
}
