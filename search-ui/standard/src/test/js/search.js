/*jshint strict:false*/
/*global CasperError, console, phantom, require, casper*/
casper.test.begin('simple contextual query', 3, function(test) {
    casper.start('http://localhost:8383/?sync=true');

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('form').length === 1;
        });
    }, function() {
        test.pass('Found query form');
    }, function() {
        test.fail('Failed finding query form');
    });

    casper.then(function () {
        this.fill('form#searchForm', { q: "canada" }, false);
    });

    casper.thenClick('.searchButton');

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('#high-results').length != 0;
        });
    }, function() {
        test.pass('Executed search');
    }, function() {
        test.fail('Failed search');
    });

    casper.then(function() {
        // aggregate results for the 'phantomjs' search
        this.test.assertEval(function() {
            return __utils__.findAll('a.metacard-link').length >= 10;
        }, 'Search for contextual query retrieves 10 or more results');
    });

    casper.run(function() {
        test.done();
    });
});
