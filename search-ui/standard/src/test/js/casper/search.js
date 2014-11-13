/*jshint strict:false*/
/*global document, casper*/
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

    //we need a mock cometd server to be able to run this

    casper.then(function () {
        this.fillSelectors('form#searchForm', { 'input[name="q"]': 'canada' }, false);
    });

    casper.thenClick('.searchButton');

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('#low-count').length !== 0;
        });
    }, function() {
        test.pass('Executed search');
    }, function() {
        test.fail('Failed search');
    });

    casper.waitFor(function(){
        return this.evaluate(function(){
            return document.querySelectorAll('a.metacard-link').length >= 10;
        });
    }, function() {
        test.pass('Search for contextual query retrieves 10 or more results');
    }, function() {
        test.fail('Search failed to find 10 or more results');
    });
    
    casper.run(function() {
        test.done();
    });
});
