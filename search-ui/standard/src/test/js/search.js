/*jshint strict:false*/
/*global CasperError, console, phantom, require, casper*/
// NOTE: to enable debug uncomment the following 2 lines.
//casper.options.verbose = true;
//casper.options.logLevel = 'debug';
casper.test.begin('simple contextual query', 3, function(test) {
    casper.start('http://localhost:8383/');

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('form').length === 1;
        });
    }, function() {
        test.pass('Found query form');
    }, function() {
        test.fail('Failed finding query form');
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            var Backbone = require('backbone');
            Backbone.generateGuid = function(model) {
                return 'e863f023-3b6f-4575-badf-a1f114e7b378';
            };
            return true;
        });
    });

    casper.then(function () {
        this.fill('form#searchForm', { q: "canada" }, false);
    });

    casper.thenClick('.searchButton');

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('#low-count').length != 0;
        });
    }, function() {
        test.pass('Executed search');
    }, function() {
        test.fail('Failed search');
    });
    
    casper.waitFor(function(){
        return this.evaluate(function(){
            return document.querySelectorAll('a.metacard-link').length >= 10;
        })
    }, function() {
        test.pass('Search for contextual query retrieves 10 or more resutls');
    }, function() {
        test.fail('Search failed to find 10 or more results');
    });
    
    casper.run(function() {
        test.done();
    });
});
