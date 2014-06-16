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
//var x = require('casper').selectXPath;
casper.options.viewportSize = {width: 2452, height: 868};
casper.test.begin('Configuration View test', function(test) {
    casper.start('http://localhost:8383');

    casper.waitForSelector('#welcome',
    function success() {
        test.pass('Found welcome container div');
    },
    function fail() {
        test.fail('Did not find welcome container div');
    });

    casper.waitForSelector('#configuration',
    function success() {
        test.pass('Found configuration container div');
    },
    function fail() {
        test.fail('Did not find configuration container div');
    });

    casper.waitFor(function() {
       return this.evaluate(function() {
           return document.querySelectorAll('#startStep').length === 1;
       });
    }, function() {
       test.assertExists('#startStep');
       this.click('#startStep');
    }, function() {
       test.fail('Failed start button');
    });

    casper.waitFor(function() {
      return this.evaluate(function() {
          return document.querySelectorAll('#config-form').length === 1;
      });
    }, function() {
      test.pass('Found config form');
    }, function() {
      test.fail('Failed finding config form');
    });

    casper.then(function () {
        this.fill('form#config-form', { host: "" }, false);
    });

    casper.waitFor(function() {
       return this.evaluate(function() {
           return document.querySelectorAll('#nextStep').length === 1;
       });
    }, function() {
       test.assertExists('#nextStep');
       this.click('#nextStep');
    }, function() {
       test.fail('Failed next button');
    });

    casper.waitFor(function() {
      return this.evaluate(function() {
          return document.querySelectorAll('#config-form').length === 1;
      });
    }, function() {
      test.pass('Could not submit invalid hostname');
    }, function() {
      test.fail('Submitted valid hostname');
    });

    casper.then(function () {
        this.fill('form#config-form', { host: "localhost" }, false);
    });

    casper.then(function () {
        this.fill('form#config-form', { port: "blah" }, false);
    });

    casper.waitFor(function() {
       return this.evaluate(function() {
           return document.querySelectorAll('#nextStep').length === 1;
       });
    }, function() {
       test.assertExists('#nextStep');
       this.click('#nextStep');
    }, function() {
       test.fail('Failed next button');
    });

    casper.waitFor(function() {
      return this.evaluate(function() {
          return document.querySelectorAll('#config-form').length === 1;
      });
    }, function() {
      test.pass('Could not submit incorrect port value');
    }, function() {
      test.fail('Submitted incorrect port value');
    });

    casper.then(function () {
        this.fill('form#config-form', { port: "" }, false);
    });

    casper.waitFor(function() {
       return this.evaluate(function() {
           return document.querySelectorAll('#nextStep').length === 1;
       });
    }, function() {
       test.assertExists('#nextStep');
       this.click('#nextStep');
    }, function() {
       test.fail('Failed next button');
    });

    casper.waitFor(function() {
      return this.evaluate(function() {
          return document.querySelectorAll('#config-form').length === 1;
      });
    }, function() {
      test.pass('Could not submit incorrect port value');
    }, function() {
      test.fail('Submitted incorrect port value');
    });

    casper.then(function () {
        this.fill('form#config-form', { port: "8181" }, false);
    });

    casper.waitFor(function() {
       return this.evaluate(function() {
           return document.querySelectorAll('#nextStep').length === 1;
       });
    }, function() {
       test.assertExists('#nextStep');
       this.click('#nextStep');
    }, function() {
       test.fail('Failed next button');
    });

    casper.waitFor(function() {
      return this.evaluate(function() {
          return document.querySelectorAll('#config-form').length === 0;
      });
    }, function() {

      test.pass('Submitted correct form value and continued');
    }, function() {
      test.fail('Submitted correct form value but did not continue');
    });

   casper.run(function() {test.done();});
});