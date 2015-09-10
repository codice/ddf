/*jslint node: true */
/* global describe, it, require */
'use strict';

var shared = require('./shared');
var asserters = shared.asserters;

describe('Installation', function () {
    shared.setup(this);

    describe('welcome page', function () {
        it("should contain welcome", function () {
            return this.browser
                .waitForElementById('welcome', asserters.textInclude('Welcome'), shared.timeout);
        });

        it('should contain insecure defaults', function () {
            return this.browser
                .waitForElementById('accordion', shared.timeout)
                .waitForElementById('details', asserters.textInclude('Show details'), shared.timeout).click()
                .waitForElementById('collapseAlerts', asserters.isDisplayed, shared.timeout)
                .waitForElementById('details', asserters.textInclude('Hide details'), shared.timeout).click()
                .waitForElementById('collapseAlerts', shared.timeout)
        });

        it('should contain start button', function () {
            return this.browser
                .waitForElementById('startStep', shared.timeout).click();
        });
    });

    describe('configure general settings page', function () {
        it('should contain configure general settings', function () {
            return this.browser
                .waitForElementById('configuration', asserters.textInclude('Configure general settings'), shared.timeout);
        });

        // Check for protocols
        it('should contain http and https protocols', function() {
            return this.browser
                .waitForElementByClassName('dropdown-toggle', asserters.textInclude('https'), shared.timeout).click()
                .waitForElementByCssSelector('input[value="http://"]').click()
                .waitForElementByClassName('dropdown-toggle', !asserters.textInclude('https'), shared.timeout).click()
                .waitForElementByCssSelector('input[value="https://"]').click()
                .waitForElementByClassName('dropdown-toggle', asserters.textInclude('https'), shared.timeout);
        });

        // Check for host/port/site name/ organization
        it('should contain host, port, site name and organization text fields', function() {
            return this.browser
                .waitForElementByName('host', shared.timeout)
                .waitForElementByName('port', shared.timeout)
                .waitForElementByName('id', shared.timeout)
                .waitForElementByName('organization', shared.timeout);
        });

        it('should contain next button', function () {
            return this.browser
                .waitForElementById('nextStep', shared.timeout).click();
        });
    });

    describe('configure anonymous claims attributes  page', function () {
        it('should contain configure general settings', function () {
            return this.browser
                .waitForElementById('anonClaims', asserters.textInclude('Configure anonymous claims attributes'), shared.timeout);
        });

        it('should contain a profile dropdown', function() {
            return this.browser
                .waitForElementByCssSelector('button[title="Default"]', shared.timeout).click()
                .waitForElementByClassName('dropdown-menu', asserters.isDisplayed, shared.timeout)
                .waitForElementById('anonClaims', shared.timeout).click()
        });

        it('should have the default attributes', function() {
            return this.browser
                .waitForConditionInBrowser("document.getElementsByClassName('claim-table')[0].rows.length === 3", shared.timeout)
                .waitForElementByCssSelector('input[value="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"]', shared.timeout)
                .waitForElementByCssSelector('input[value="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role"]', shared.timeout);
        });

        it('should have defaults for dropdown', function() {
            return this.browser
                .waitForElementByClassName('fa-caret-down', shared.timeout).click()
                .waitForElementByClassName('dropdown-menu', asserters.isDisplayed, shared.timeout)
                .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('attribute-dropdown.png'))
                .waitForElementByCssSelector('li[value="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"]', shared.timeout)
                .waitForElementByCssSelector('li[value="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname"]', shared.timeout)
                .waitForElementByCssSelector('li[value="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"]', shared.timeout)
                .waitForElementByCssSelector('li[value="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role"]', shared.timeout)
                .waitForElementByCssSelector('li[value="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname"]', shared.timeout)
                .waitForElementByCssSelector('li[value="Add Custom Attribute..."]', shared.timeout)
                .waitForElementById('anonClaims', shared.timeout).click();
        });

        it('should be able to add and remove attributes', function() {
            return this.browser
                .waitForElementByLinkText('Add Attribute', shared.timeout).click()
                .waitForConditionInBrowser("document.getElementsByClassName('claim-table')[0].rows.length === 4", shared.timeout)
                .waitForElementsByClassName('minus-button', shared.timeout).last().click()
                .waitForConditionInBrowser("document.getElementsByClassName('claim-table')[0].rows.length === 3", shared.timeout);
        });

        it('should have an error when clicking next with empty attribute', function() {
            return this.browser
                .waitForElementByLinkText('Add Attribute', shared.timeout).click()
                .waitForElementById('nextStep', shared.timeout).click()
                .waitForElementById('anonClaims', asserters.textInclude('Configure anonymous claims attributes'), shared.timeout)
                .waitForElementByName('claimName3Error', asserters.textInclude('Claim name is required'), shared.timeout)
                .waitForElementByName('claimValue3Error', asserters.textInclude('Claim value is required'), shared.timeout)
                .waitForElementsByClassName('minus-button', shared.timeout).last().click();
        });

        // Refreshes the page and goes back because typing in the claimValue doesn't work
        it('should show warnings when removing default attributes', function() {
            return this.browser
                .waitForElementByClassName('minus-button', shared.timeout).click()
                .waitForElementByClassName('minus-button', shared.timeout).click()
                .waitForElementById('warning-div', asserters.textInclude('By removing the required claim http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier, anonymous access will effectively be disabled'), shared.timeout)
                .waitForElementById('warning-div', asserters.textInclude('By removing the required claim http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role, anonymous access will effectively be disabled'), shared.timeout)
                .refresh()
                .waitForElementById('startStep', shared.timeout).click()
                .waitForElementById('nextStep', shared.timeout).click();
        });

        it('should show warnings for duplicate attributes', function() {
            return this.browser
                .waitForElementByLinkText('Add Attribute', shared.timeout).click()
                .waitForElementsByName('claimName', shared.timeout).last().type('http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier')
                .waitForElementById('anonClaims', shared.timeout).click()
                .waitForElementById('warning-div', asserters.isDisplayed, shared.timeout)
                .waitForElementsByClassName('minus-button', shared.timeout).last().click();
        });

        it('should contain next button', function () {
            return this.browser
                .waitForElementById('nextStep', shared.timeout).click();
        });
    });

    describe('setup types page', function () {
        it('should contain Setup Types', function () {
            return this.browser
                .waitForElementById('profiles', asserters.textInclude('Setup Types'), shared.timeout);
        });

        it('should contain standard and full', function () {
            return this.browser
                .waitForElementById('profiles', asserters.textInclude('Standard'), shared.timeout)
                .waitForConditionInBrowser("$(document.getElementsByClassName('selected')[0]).text().indexOf('standard') !== -1", shared.timeout)
                .waitForElementByCssSelector('input[value="profile-full"]').click()
                .waitForConditionInBrowser("$(document.getElementsByClassName('selected')[0]).text().indexOf('full') !== -1", shared.timeout)
                .waitForElementByCssSelector('input[value="profile-standard"]').click()
                .waitForConditionInBrowser("$(document.getElementsByClassName('selected')[0]).text().indexOf('standard') !== -1", shared.timeout)
                .waitForElementByCssSelector('.customize', shared.timeout).click();
        });

        describe('customize install', function () {
            it('should contain header and contain the apps', function() {
                return this.browser
                    .waitForElementByClassName('main-content', asserters.textInclude('Select applications to setup'), shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('span.appitem').length === 9", shared.timeout);
            });

            it('should have all apps except opendj selected', function() {
                return this.browser
                    .waitForConditionInBrowser("document.querySelectorAll('input#opendj-embeddedcb:checked').length == 0", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#platform-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#catalog-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#content-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#search-ui-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#spatial-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#solr-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#security-services-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#admin-appcb:checked').length == 1", shared.timeout);
            });

            it('should deselect all apps under ddf-catalog when ddf-catalog is deselected', function() {
                return this.browser
                    .waitForElementById('catalog-appcb', shared.timeout).click()
                    .waitForConditionInBrowser("document.querySelectorAll('input#opendj-embeddedcb:checked').length == 0", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#platform-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#catalog-appcb:checked').length == 0", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#content-appcb:checked').length == 0", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#search-ui-appcb:checked').length == 0", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#spatial-appcb:checked').length == 0", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#solr-appcb:checked').length == 0", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#security-services-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#admin-appcb:checked').length == 1", shared.timeout);
            });

            it('should select the catalog app when selecting the solr app', function() {
                return this.browser
                    .waitForConditionInBrowser("document.querySelectorAll('input#catalog-appcb:checked').length == 0", shared.timeout)
                    .waitForElementById('solr-appcb', shared.timeout).click()
                    .waitForConditionInBrowser("document.querySelectorAll('input#solr-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#catalog-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#platform-appcb:checked').length == 1", shared.timeout)
                    .waitForElementById('catalog-appcb', shared.timeout).click();
            });

            it('should select the catalog app and search ui when selecting the spatial app', function() {
                return this.browser
                    .waitForConditionInBrowser("document.querySelectorAll('input#catalog-appcb:checked').length == 0", shared.timeout)
                    .waitForElementById('spatial-appcb', shared.timeout).click()
                    .waitForConditionInBrowser("document.querySelectorAll('input#spatial-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#search-ui-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#catalog-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#platform-appcb:checked').length == 1", shared.timeout)
                    .waitForElementById('catalog-appcb', shared.timeout).click();
            });

            it('should select the catalog app when selecting the search app', function() {
                return this.browser
                    .waitForConditionInBrowser("document.querySelectorAll('input#catalog-appcb:checked').length == 0", shared.timeout)
                    .waitForElementById('search-ui-appcb', shared.timeout).click()
                    .waitForConditionInBrowser("document.querySelectorAll('input#search-ui-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#catalog-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#platform-appcb:checked').length == 1", shared.timeout)
                    .waitForElementById('catalog-appcb', shared.timeout).click();
            });

            it('should select the catalog app when selecting the content app', function() {
                return this.browser
                    .waitForConditionInBrowser("document.querySelectorAll('input#catalog-appcb:checked').length == 0", shared.timeout)
                    .waitForElementById('content-appcb', shared.timeout).click()
                    .waitForConditionInBrowser("document.querySelectorAll('input#content-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#catalog-appcb:checked').length == 1", shared.timeout)
                    .waitForConditionInBrowser("document.querySelectorAll('input#platform-appcb:checked').length == 1", shared.timeout);
            });

            it('should display details when hovering over an app', function() {
                return this.browser
                    .waitForElementById('solr-appcb', shared.timeout).moveTo()
                    .waitForElementByCssSelector('td#detailsName', shared.timeout)
                    .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('hover.png'))
                    .waitForElementById('detailsName', asserters.textInclude('DDF Solr Catalog'), shared.timeout)
                    .waitForElementById('detailsVersion', asserters.textNonEmpty, shared.timeout)
                    .waitForElementById('detailsDesc', asserters.textNonEmpty, shared.timeout);
            });

            it('should remove the hover when the mouse moves off the app', function() {
                return this.browser
                    .waitForElementById('tabs', shared.timeout).moveTo()
                    .waitForConditionInBrowser("document.querySelectorAll('td#detailsName').length == 0", shared.timeout)
            });

            it('should contain next button', function () {
                return this.browser
                    .waitForElementById('spatial-appcb', shared.timeout).click()
                    .waitForElementById('solr-appcb', shared.timeout).click()
                    .waitForElementById('nextStep', shared.timeout).click();
            });
        });
    });

    describe('finish page', function () {
        it('should have finished', function() {
            return this.browser
                .waitForElementById('finish', asserters.textInclude('Finished'), shared.timeout);
        });

        it('should have finish button', function() {
            return this.browser
                .waitForElementById('finishStep', asserters.textInclude('Finish'), shared.timeout);
        });
    });
});