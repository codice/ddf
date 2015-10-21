/*jslint node: true */
/* global describe, it, require */
'use strict';

var shared = require('./shared');

var convertLLtoUSNG = function(browser,lat1, lon1, lat2, lon2) {
    return browser
        .waitForElementById('latlon', shared.timeout).click()
        .waitForElementById('locationBbox', shared.timeout).click()
        .waitForElementById('mapNorth', shared.timeout).clear().type(lat1)
        .waitForElementById('mapSouth', shared.timeout).clear().type(lat2)
        .waitForElementById('mapEast', shared.timeout).clear().type(lon1)
        .waitForElementById('mapWest', shared.timeout).clear().type(lon2)
        .waitForElementById('usng', shared.timeout).click()
        .waitForElementById('usngbb', shared.timeout);
};

var convertUSNGtoLL = function(browser, usng) {
    return browser
        .waitForElementById('usng', shared.timeout).click()
        .waitForElementById('locationBbox', shared.timeout).click()
        .waitForElementById('usngbb', shared.timeout).type(usng)
        .waitForElementById('latlon', shared.timeout).click();
};


describe('USNG Search', function () {
    shared.setup(this);

    // Create bigger and bigger boxes around the washington monument
    // Tests the precision conversion from llbbox to usng
    describe('lat/lon bbox to usng', function() {

        // 0-1m
        it("should convert to 18S UJ 23495 06472", function () {
            var lat = 38.8894;
            var lon = -77.0351;
            var usng = "18S UJ 23495 06472";
            return convertLLtoUSNG(this.browser, lat, lon, lat, lon)
                .waitForConditionInBrowser("document.getElementById('usngbb').value.includes('" + usng + "')", shared.timeout)
                .refresh();
        });

        // 2-10m
        it("should convert to 18S UJ 2349 0648", function () {
            var north = 38.8895;
            var south = 38.8895;
            var east = -77.0352;
            var west = -77.0351;
            var usng = "18S UJ 2349 0648";
            return convertLLtoUSNG(this.browser, north, east, south, west)
                .waitForConditionInBrowser("document.getElementById('usngbb').value.includes('" + usng + "')", shared.timeout)
                .refresh();
        });

        // 11-100m
        it("should convert to 18S UJ 234 064", function () {
            var north = 38.8896;
            var south = 38.8895;
            var east = -77.0357;
            var west = -77.0361;
            var usng = "18S UJ 234 064";
            return convertLLtoUSNG(this.browser, north, east, south, west)
                .waitForConditionInBrowser("document.getElementById('usngbb').value.includes('" + usng + "')", shared.timeout)
                .refresh();
        });

            // 100-1000m
        it("should convert to 18S UJ 23 06", function () {
            var north = 38.8905;
            var south = 38.8891;
            var east = -77.0355;
            var west = -77.0376;
            var usng = "18S UJ 23 06";
            return convertLLtoUSNG(this.browser, north, east, south, west)
                .waitForConditionInBrowser("document.getElementById('usngbb').value.includes('" + usng + "')", shared.timeout)
                .refresh();
        });

            // 1000-10k
        it("should convert to 18S UJ 2 0", function () {
            var north = 38.8973;
            var south = 38.8825;
            var east = -77.0241;
            var west = -77.0429;
            var usng = "18S UJ 2 0";
            return convertLLtoUSNG(this.browser, north, east, south, west)
                .waitForConditionInBrowser("document.getElementById('usngbb').value.includes('" + usng + "')", shared.timeout)
                .refresh();
        });

            // 10k-100k
        it("should convert to 18S UJ", function () {
            var north = 38.8973;
            var south = 38.8825;
            var east = -77.0241;
            var west = -77.0429;
            var usng = "18S UJ";
            return convertLLtoUSNG(this.browser, north, east, south, west)
                .waitForConditionInBrowser("document.getElementById('usngbb').value.includes('" + usng + "')", shared.timeout)
                .refresh();
        });

            // 100k+
        it("should convert to 18S", function () {
            var north = 40;
            var south = 32;
            var east = -72;
            var west = -78;
            var usng = "18S";
            return convertLLtoUSNG(this.browser, north, east, south, west)
                .waitForConditionInBrowser("document.getElementById('usngbb').value.includes('" + usng + "')", shared.timeout)
                .refresh();
        });
    });

    // Create bigger and bigger boxes around the washington monument
    // Tests the precision conversion from usng to llbbox
    describe('usng to lat/lon bbox', function () {

        it("should convert to 38.889 -77.0351", function () {
            var north = 38.889;
            var south = 38.889;
            var east = -77.0351;
            var west = -77.0351;
            return convertUSNGtoLL(this.browser, "18S UJ 23487 06483")
                .waitForConditionInBrowser("document.getElementById('mapWest').value.indexOf(\'"+west+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapEast').value.indexOf(\'"+east+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapNorth').value.indexOf(\'"+north+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapSouth').value.indexOf(\'"+south+"\') >= 0", shared.timeout);
        });

        it("should convert to 38.8895 -77.0351 38.8894 -77.0350", function () {
            var north = 38.8895;
            var south = 38.8894;
            var east = -77.0350;
            var west = -77.0351;
            return convertUSNGtoLL(this.browser, "18S UJ 2349 0648")
                .waitForConditionInBrowser("document.getElementById('mapWest').value.indexOf(\'"+west+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapEast').value.indexOf(\'"+east+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapNorth').value.indexOf(\'"+north+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapSouth').value.indexOf(\'"+south+"\') >= 0", shared.timeout);
        });

        it("should convert to 38.8896 -77.0361 38.8887 -77.0350", function () {
            var north = 38.8896;
            var south = 38.8887;
            var east = -77.0350;
            var west = -77.0361;
            return convertUSNGtoLL(this.browser, "18S UJ 234 064")
                .waitForConditionInBrowser("document.getElementById('mapWest').value.indexOf(\'"+west+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapEast').value.indexOf(\'"+east+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapNorth').value.indexOf(\'"+north+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapSouth').value.indexOf(\'"+south+"\') >= 0", shared.timeout);
        });

        it("should convert to 38.8942 -77.0406 38.8850 -77.0294", function () {
            var north = 38.8942;
            var south = 38.8850;
            var east = -77.0294;
            var west = -77.0406;
            return convertUSNGtoLL(this.browser, "18S UJ 23 06")
                .waitForConditionInBrowser("document.getElementById('mapWest').value.indexOf(\'"+west+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapEast').value.indexOf(\'"+east+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapNorth').value.indexOf(\'"+north+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapSouth').value.indexOf(\'"+south+"\') >= 0", shared.timeout);
        });

        it("should convert to 38.9224 -77.0736 38.8304 -76.9610", function () {
            var north = 38.9224;
            var south = 38.8304;
            var east = -76.9610;
            var west = -77.0736;
            return convertUSNGtoLL(this.browser, "18S UJ 2 0")
                .waitForConditionInBrowser("document.getElementById('mapWest').value.indexOf(\'"+west+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapEast').value.indexOf(\'"+east+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapNorth').value.indexOf(\'"+north+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapSouth').value.indexOf(\'"+south+"\') >= 0", shared.timeout);
        });

        it("should convert to 39.7440 -77.3039 38.8260 -76.1671", function () {
            var north = 39.7440;
            var south = 38.8260;
            var east = -76.1671;
            var west = -77.3039;
            return convertUSNGtoLL(this.browser, "18S UJ")
                .waitForConditionInBrowser("document.getElementById('mapWest').value.indexOf(\'"+west+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapEast').value.indexOf(\'"+east+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapNorth').value.indexOf(\'"+north+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapSouth').value.indexOf(\'"+south+"\') >= 0", shared.timeout);
        });

        it("should convert to 40 -84 32 -84", function () {
            var north = 40;
            var south = 32;
            var east = -78;
            var west = -84;
            return convertUSNGtoLL(this.browser, "17S")
                .waitForConditionInBrowser("document.getElementById('mapWest').value.indexOf(\'"+west+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapEast').value.indexOf(\'"+east+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapNorth').value.indexOf(\'"+north+"\') >= 0", shared.timeout)
                .waitForConditionInBrowser("document.getElementById('mapSouth').value.indexOf(\'"+south+"\') >= 0", shared.timeout);
        });
    });
});
