/*jslint node: true */
/* global describe, it, require */
'use strict';

var shared = require('./shared');

//var asserters = shared.asserters;

//var essentiallyEqual = function(/* float */ a, /* float */ b, /* float */ epsilon) {
//    var A = Math.abs(a), B = Math.abs(b);
//    return Math.abs(A - B) < epsilon;
//}

var convertLLtoUSNG = function(browser,lat1, lon1, lat2, lon2) {
    return browser
        .waitForElementById('latlon', shared.timeout).click()
        .waitForElementById('locationBbox', shared.timeout).click()
        .waitForElementById('north', shared.timeout).type(lat1)
        .waitForElementById('south', shared.timeout).type(lat2)
        .waitForElementById('east', shared.timeout).type(lon1)
        .waitForElementById('west', shared.timeout).type(lon2)
        .waitForElementById('usng', shared.timeout).click()
        .waitForElementById('usngbb', shared.timeout);
};

var convertUSNGtoLL = function(browser, usng) {
    return browser
        .waitForElementById('usng', shared.timeout).click()
        .waitForElementById('locationBbox', shared.timeout).click()
        .waitForElementById('usngbb', shared.timeout).type(usng)
        .waitForElementById('latlon', shared.timeout).click()
        .takeScreenshot().saveScreenshot(shared.getPathForScreenshot('USNGtoLL2.png'));
};

describe('USNG Search', function () {
    shared.setup(this);

    describe('lat/lon bbox to usng', function () {
        it("should convert to 18S UJ 23487 06483", function () {
            var lat = 38.88;
            var lon = -77.03;
            //var usng = "18S UJ 23487 06483";
            return convertLLtoUSNG(this.browser, lat, lon, lat, lon);
               // .waitForElementById('usngbb', asserters.textInclude(usng), shared.timeout);
        });

        it("should convert to 38.889 -77.0351", function () {
            var north = 38.889;
            var south = 38.889;
            var east = -77.0351;
            var west = -77.0351;
            return convertUSNGtoLL(this.browser, "18S UJ 23487 06483")
                .waitForConditionInBrowser("document.getElementById('west').value.indexOf(\'"+west+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('east').value.indexOf(\'"+east+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('north').value.indexOf(\'"+north+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('south').value.indexOf(\'"+south+"\') >= 0", shared.timeout);
        });

        it("should convert to 38.8895 -77.0351 38.8894 -77.0350", function () {
            var north = 38.8895;
            var south = 38.8894;
            var east = -77.0350;
            var west = -77.0351;
            return convertUSNGtoLL(this.browser, "18S UJ 2349 0648")
                .waitForConditionInBrowser("document.getElementById('west').value.indexOf(\'"+west+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('east').value.indexOf(\'"+east+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('north').value.indexOf(\'"+north+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('south').value.indexOf(\'"+south+"\') >= 0", shared.timeout);
        });

        it("should convert to 38.8896 -77.0361 38.8887 -77.0350", function () {
            var north = 38.8896;
            var south = 38.8887;
            var east = -77.0350;
            var west = -77.0361;
            return convertUSNGtoLL(this.browser, "18S UJ 234 064")
                .waitForConditionInBrowser("document.getElementById('west').value.indexOf(\'"+west+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('east').value.indexOf(\'"+east+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('north').value.indexOf(\'"+north+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('south').value.indexOf(\'"+south+"\') >= 0", shared.timeout);
        });

        it("should convert to 38.8942 -77.0406 38.8850 -77.0294", function () {
            var north = 38.8942;
            var south = 38.8850;
            var east = -77.0294;
            var west = -77.0406;
            return convertUSNGtoLL(this.browser, "18S UJ 23 06")
                .waitForConditionInBrowser("document.getElementById('west').value.indexOf(\'"+west+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('east').value.indexOf(\'"+east+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('north').value.indexOf(\'"+north+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('south').value.indexOf(\'"+south+"\') >= 0", shared.timeout);
        });

        it("should convert to 38.9224 -77.0736 38.8304 -76.9610", function () {
            var north = 38.9224;
            var south = 38.8304;
            var east = -76.9610;
            var west = -77.0736;
            return convertUSNGtoLL(this.browser, "18S UJ 2 0")
                .waitForConditionInBrowser("document.getElementById('west').value.indexOf(\'"+west+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('east').value.indexOf(\'"+east+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('north').value.indexOf(\'"+north+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('south').value.indexOf(\'"+south+"\') >= 0", shared.timeout);
        });

        it("should convert to 39.7440 -77.3039 38.8260 -76.1671", function () {
            var north = 39.7440;
            var south = 38.8260;
            var east = -76.1671;
            var west = -77.3039;
            return convertUSNGtoLL(this.browser, "18S UJ")
                .waitForConditionInBrowser("document.getElementById('west').value.indexOf(\'"+west+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('east').value.indexOf(\'"+east+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('north').value.indexOf(\'"+north+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('south').value.indexOf(\'"+south+"\') >= 0", shared.timeout);
        });

        it("should convert to 40 -84 32 -84", function () {
            var north = 40;
            var south = 32;
            var east = -78;
            var west = -84;
            return convertUSNGtoLL(this.browser, "17S")
                .waitForConditionInBrowser("document.getElementById('west').value.indexOf(\'"+west+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('east').value.indexOf(\'"+east+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('north').value.indexOf(\'"+north+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('south').value.indexOf(\'"+south+"\') >= 0", shared.timeout);
        });
    });
});
