/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*jshint strict:false*/
/*global CasperError, console, phantom, require, casper*/
casper.options.viewportSize = {width: 2452, height: 868};

casper.test.begin('Application Selection View test', function(test) {
    casper.start('http://localhost:8383');

//Checking containers
    casper.then(function() {
        test.comment("Checking for all the main div containers");
    });

//Checking clickable buttons
    casper.then(function() {
        test.comment("Testing out clickable buttons");
    });

//Tests originally designed from the Installer module that tests out tree functionality
    casper.then(function() {
        test.comment("Testing out app tree functionality");
    });

    casper.waitWhileSelector('td#detailsName',
        function success(){
            test.pass('Spatial application details hidden');
        },
        function fail() {
            test.fail('Spatial application details did not disappear');
        }
    );

//Checking Add/Upgrade clickable buttons
    casper.then(function() {
        test.comment("Testing out Add/Upgrade clickable buttons ");
    });

//Check Maven URL functionality
    casper.then(function() {
        test.comment("Checking Maven URL Upload functionality");
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            return $('span.file-fail-text').html() !== '';
        });
    }, function() {
        test.pass('Entered an invalid Maven URL');
    }, function() {
        test.fail('Entered a correct Maven URL');
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('li.url-list-item').length === 0;
        });
    }, function() {
        test.pass('List of Maven URLs is empty');
    }, function() {
        test.fail('List of Maven URLs has entries');
    });

    casper.then(function () {
        this.evaluate(function() {
            $('input#urlField').val('mvn:groupId:artifactId/version/xml/features');
        });
    });

    casper.run(function() {test.done();});
});
