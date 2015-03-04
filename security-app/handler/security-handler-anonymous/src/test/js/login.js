/*jshint strict:false*/
/*global CasperError, console, phantom, require, casper*/
// NOTE: to enable debug uncomment the following 2 lines.
//casper.options.verbose = true;
//casper.options.logLevel = 'debug';
casper.test.begin('Login test', 1, function(test) {
    casper.start('http://localhost:8383/');

        casper.waitFor(function() {

            // Make sure that we got the loginForm
            return this.evaluate(function() {
                return document.querySelectorAll('form[id="loginForm"]').length === 1;
            });
        }, function() {
            test.pass('Found login form');
        }, function() {
            test.fail('Failed finding login form');
        });

        casper.run(function() {
            test.done();
        });
});
