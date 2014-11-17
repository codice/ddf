/*jslint node: true */
/* global describe, it, require */
'use strict';

var shared = require('./shared');

describe('Contextual', function () {
    shared.setup(this);

    describe('Wildcard query', function () {
        shared.reloadBeforeTests();

        it("should allow search", function () {
            return this.browser
                .waitForElementByCssSelector('form#searchForm input[name="q"]', 10000)
                .elementByCssSelector('input[name="q"]').type('*')
                .elementById('searchButton').click();
        });

        it("should show result count", function () {
            return this.browser
                .waitForElementByClassName('result-count', 10000)
                .then(function (count) {
                    count.text().should.eventually.contain('results displayed');
                });
        });

        it("should display results", function () {
            return this.browser
                .waitForElementsByCssSelector('a.metacard-link', 10000)
                .waitForConditionInBrowser('document.querySelectorAll("a.metacard-link").length >= 10', 10000);
        });
    });
});
