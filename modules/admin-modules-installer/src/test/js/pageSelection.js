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
casper.test.begin('Page Selection Test', function(test) {
    casper.start('http://localhost:8383');

//Checking Installer Module Steps/Pages
    casper.then(function() {
        test.comment("Checking for all Installer Module Steps/Pages");
    });
    casper.waitForSelector('#welcome',
    function success() {
        test.pass('Found Welcome container');
    },
    function fail() {
        test.fail('Did NOT find Welcome container');
    });

    casper.waitForSelector('#configuration',
    function success() {
        test.pass('Found Configuration container');
    },
    function fail() {
        test.fail('Did NOT find Configuration container');
    });

    casper.waitForSelector('#applications',
    function success() {
        test.pass('Found Applications container');
    },
    function fail() {
        test.fail('Did NOT find Applications container');
    });

    casper.waitForSelector('#finish',
    function success() {
        test.pass('Found Finish container');
    },
    function fail() {
        test.fail('Did NOT find Finish container');
    });

    casper.waitForSelector('#navigation',
    function success() {
        test.pass('Found Navigation container');
    },
    function fail() {
        test.fail('Did NOT find Navigation container');
    });

//Testing out navigation buttons
    casper.then(function() {
        test.comment("Testing out all the navigational buttons");
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
            return document.querySelectorAll('#previousStep').length === 1;
        });
    }, function() {
        test.assertExists('#previousStep');
        this.click('#previousStep');
    }, function() {
        test.fail('Failed previous button');
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
           return document.querySelectorAll('#previousStep').length === 1;
       });
    }, function() {
       test.assertExists('#previousStep');
       this.click('#previousStep');
    }, function() {
       test.fail('Failed previous button');
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
           return document.querySelectorAll('#previousStep').length === 1;
       });
    }, function() {
       test.assertExists('#previousStep');
       this.click('#previousStep');
    }, function() {
       test.fail('Failed previous button');
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
            return document.querySelectorAll('#finishStep').length === 1;
        });
    }, function() {
        test.assertExists('#finishStep');
        this.click('#finishStep');
    }, function() {
        test.fail('Failed finish button');
    });

   casper.run(function() {test.done();});
});