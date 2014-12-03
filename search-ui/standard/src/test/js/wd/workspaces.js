/*jslint node: true */
/* global describe, it, require */
'use strict';

var shared = require('./shared');
var asserters = shared.asserters;

describe('Workspace', function () {
    shared.setup(this);

    it("should show workspace list", function () {
        return this.browser
            .waitForElementByLinkText('Workspaces', asserters.isDisplayed, 20000).click()
            .waitForElementByClassName('workspace-table');
    });

    it("should allow adding a workspace", function () {
        return this.browser
            .elementById('Add').click()
            .waitForElementById('workspaceName').type('foo')
            .elementByCssSelector('#workspaceAddForm a.submit').click()
            .waitForElementByClassName('workspace-name')
            .text().should.eventually.equal('foo')
            .elementByClassName('workspace-row')
            .click();
    });

    it("should allow adding a search to a workspace", function () {
        return this.browser
            .waitForElementById('addSearch').click()
            .waitForElementById('queryName').type('bar')
            .waitForElementByCssSelector('#workspaces input[name="q"]').type('*')
            .waitForElementById('workspaceSearchButton')
            .safeExecute('document.querySelectorAll("#workspaceSearchButton")[0].scrollIntoView(true)')
            .elementById('workspaceSearchButton').click()
            .waitForElementByClassName('workspace-name')
            .text().should.eventually.equal('bar');
    });

    it("should allow viewing search in workspace", function () {
        return this.browser
            .waitForElementByClassName('workspace-row').click()
            .waitForElementByCssSelector('#workspaces .result-count i', asserters.textInclude('results'), 10000)
            .waitForConditionInBrowser('document.querySelectorAll("a.metacard-link").length >= 10', 10000);
    });

    it("should allow saving results", function () {
        return this.browser
            .waitForElementById('Save').click()
            .waitForElementByCssSelector('#workspaces input.select-record-checkbox', 10000).click()
            .waitForElementById('Done').click()
            .waitForElementByCssSelector('input[value="foo"]', asserters.isDisplayed).click()
            .waitForElementByCssSelector('a.submit').click()
            .waitForElementById('Workspace').click()
            .waitForElementById('view-records').click()
            .waitForElementsByCssSelector('a.metacard-link')
            .waitForConditionInBrowser('document.querySelectorAll("a.metacard-link").length >= 1');
    });

});