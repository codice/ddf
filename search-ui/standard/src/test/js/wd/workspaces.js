/*jslint node: true */
/* global describe, it, require */
'use strict';

var shared = require('./shared');
var asserters = shared.asserters;

describe('Workspace', function () {
    shared.setup(this);

    it("should show workspace list", function () {
        return this.browser
            .waitForElementByLinkText('Workspaces', asserters.isDisplayed, shared.timeout).click()
            .waitForElementByClassName('workspace-table', shared.timeout);
    });

    it("should allow adding a workspace", function () {
        return this.browser
            .elementById('Add', asserters.isDisplayed).click()
            .waitForElementById('workspaceName', asserters.isDisplayed).type('foo')
            .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('workspace-add.png'))
            .elementByCssSelector('#workspaceAddForm a.submit').click()
            .waitForElementByClassName('workspace-name', asserters.textInclude('foo'), shared.timeout)
            .elementByClassName('workspace-row').click();
    });

    it("should allow adding a search to a workspace", function () {
        return this.browser
            .waitForElementById('addSearch').click()
            .waitForElementById('queryName', asserters.isDisplayed).type('bar')
            .waitForElementByCssSelector('#workspaces input[name="q"]').type('*')
            .waitForElementById('workspaceSearchButton')
            .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('workspace-query.png'))
            .safeExecute('document.querySelectorAll("#workspaceSearchButton")[0].scrollIntoView(true)')
            .elementById('workspaceSearchButton').click()
            .waitForConditionInBrowser('document.querySelectorAll("a.workspace-name").length === 1', shared.timeout)
            .waitForConditionInBrowser('document.querySelectorAll(".fa-spin").length === 0', shared.timeout)
            .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('workspace-list.png'));
    });

    it("should allow editing searches in workspace", function () {
        return this.browser
            .waitForElementByClassName('workspace-row')
            .waitForElementById('Edit').click()
            .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('workspace-edit.png'))
            .waitForElementById('Done').click();
    });

    it("should allow viewing search in workspace", function () {
        return this.browser
            .waitForElementByClassName('workspace-row').click()
            .waitForElementByCssSelector('#workspaces .result-count i', asserters.textInclude('results'), shared.timeout)
            .waitForConditionInBrowser('document.querySelectorAll("a.metacard-link").length >= 10', shared.timeout);
    });

    it("should allow saving results", function () {
        return this.browser
            .waitForElementByCssSelector('#refresh-icon.fa-refresh', shared.timeout)
            .waitForElementById('Save').click()
            .waitForElementByCssSelector('#workspaces input.select-record-checkbox', shared.timeout).click()
            .waitForElementById('Done').click()
            .waitForElementByCssSelector('input[value="foo"]', asserters.isDisplayed, shared.timeout).click()
            .waitForElementByCssSelector('a.submit').click()
            .waitForElementById('Workspace', shared.timeout).click()
            .waitForElementById('view-records', shared.timeout).click()
            .waitForElementsByCssSelector('a.metacard-link', shared.timeout)
            .waitForConditionInBrowser('document.querySelectorAll("a.metacard-link").length >= 1', shared.timeout);
    });

});