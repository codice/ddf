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

    casper.waitForSelector('#applications',
    function success() {
        test.pass('Found applications container');
    },
    function fail() {
        test.fail('Did NOT find applications container');
    });

    casper.waitForSelector('#application-view',
    function success() {
        test.pass('Found application-view container');
    },
    function fail() {
        test.fail('Did NOT find application-view container');
    });

    casper.waitForSelector('#new-app-container',
    function success() {
        test.pass('Found new-app-container container');
    },
    function fail() {
        test.fail('Did NOT find new-app-container container');
    });

    casper.waitForSelector('#wrapper',
    function success() {
        test.pass('Found wrapper container');
    },
    function fail() {
        test.fail('Did NOT find wrapper container');
    });

    casper.waitForSelector('#apps-tree',
    function success() {
        test.pass('Found apps-tree container');
    },
    function fail() {
        test.fail('Did NOT find apps-tree container');
    });

//Checking clickable buttons
    casper.then(function() {
        test.comment("Testing out clickable buttons");
    });

    casper.waitForSelector(".btn.btn-primary.save",
        function success() {
            test.assertExists(".btn.btn-primary.save");
        },
        function fail() {
            test.assertExists(".btn.btn-primary.save");
    });

    casper.waitForSelector(".btn.btn-default.cancel",
        function success() {
            test.assertExists(".btn.btn-default.cancel");
        },
        function fail() {
            test.assertExists(".btn.btn-default.cancel");
    });

//Tests originally designed from the Installer module that tests out tree functionality
    casper.then(function() {
        test.comment("Testing out app tree functionality");
    });

    // verify that the apps-tree view shows up
    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('#apps-tree').length === 1;
        });
    }, function() {
        test.pass('Found applications tree view');
    }, function() {
        test.fail('Failed finding applications tree view');
    });

    // verify that each of the 10 applications in the test data show up in the tree
    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('span.appitem').length === 10;
        })
    }, function() {
        test.pass('Found 10 applications in tree');
    }, function() {
        test.fail('Failed to find 10 applications in tree');
    });

    // verify that selecting the solr app selects the catalog app and the platform app
    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('#solr-appcb').length === 1;
        });
    }, function() {
        test.assertExists('#solr-appcb');
        this.click('#solr-appcb');
        test.pass('Clicked the solr-app checkbox');
    }, function() {
        test.fail('Failed to locate the solr-app checkbox');
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('input#solr-appcb:checked').length == 1;
        });
    }, function() {
        test.pass('solr app checked');
    }, function() {
        test.fail('Failed to locate checked solr app');
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('input#catalog-appcb:checked').length == 1;
        });
    }, function() {
        test.pass('catalog app checked');
    }, function() {
        test.fail('Failed to locate checked catalog app');
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('input#platform-appcb:checked').length == 1;
        });
    }, function() {
        test.pass('platform app checked');
    }, function() {
        test.fail('Failed to locate checked platform app');
    });

    // verify that deselecting the catalog app deselects the solr app
    casper.then(function() {
        test.assertExists('#catalog-appcb');
        this.click('#catalog-appcb');
        test.pass('catalog app checkbox de-selected');
    });

    // wait for selections of the catalog and it's sub-children to disappear from the tree
    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('input#catalog-appcb:checked').length == 0;
        });
    }, function() {
        test.pass('catalog app unchecked');
    }, function() {
        test.fail('Failed to find unchecked catalog app');
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('input#solr-appcb:checked').length == 0;
        });
    }, function() {
        test.pass('solr app unchecked');
    }, function() {
        test.fail('Failed to find unchecked solr-app');
    });

    // verify that hovering over an app causes the details region to be displayed
    casper.then(function(){
        test.assertExists('#spatial-apptxt');
        this.mouseEvent('mouseover', '#spatial-apptxt');
        test.pass('Hovering over the spatial app');
    });

    casper.waitForSelector('td#detailsName',
        function success() {
        test.pass('Spatial application details visible.');
    }, function fail() {
        test.fail('Spatial application details not found.');
    });

    casper.then(function() {
        test.assertTrue(this.evaluate(function() {
            return document.querySelector('#detailsName').innerText == 'Spatial App';
            }
        ));
        test.assertTrue(this.evaluate(function() {
                return document.querySelector('#detailsVersion').innerText == '2.4.1-SNAPSHOT';
            }
        ));
        test.assertTrue(this.evaluate(function() {
                return document.querySelector('#detailsDesc p').innerText == 'DDF Spatial Services application default installations';
            }
        ));
    });

    // verify that not hovering cuases the details region to disappear
    casper.then(function(){
       this.mouseEvent('mouseout', '#spatial-apptxt');
        test.pass('Stopped hovering over the spatial app');
    });

    casper.waitWhileSelector('td#detailsName',
        function success(){
            test.pass('Spatial application details hidden');
        },
        function fail() {
            test.fail('Spatial application details did not disappear');
        }
    );

    // verify that app names with the version are broken apart for the display details
    casper.then(function(){
        test.assertExists('#search-app-231ALPHA3-SNAPSHOTtxt');
        this.mouseEvent('mouseover', '#search-app-231ALPHA3-SNAPSHOTtxt');
        test.pass('Hovering over the search app');
    });

    casper.waitForSelector('td#detailsName',
        function success() {
            test.pass('Search application details visible.');
        }, function fail() {
            test.fail('Search application details not found.');
        });

    casper.then(function() {
        test.assertTrue(this.evaluate(function() {
                return document.querySelector('#detailsName').innerText == 'DDF Search UI';
            }
        ));
        test.assertTrue(this.evaluate(function() {
                return document.querySelector('#detailsVersion').innerText == '2.3.1.ALPHA3-SNAPSHOT';
            }
        ));
    });

//Checking Add/Upgrade clickable buttons
    casper.then(function() {
        test.comment("Testing out Add/Upgrade clickable buttons ");
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll("a.btn.btn-primary").length === 1;
        });
    }, function() {
        test.assertExists("a.btn.btn-primary");
        this.click("a.btn.btn-primary");
    }, function() {
        test.fail('Failed to locate the Add/Upgrade button');
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll("button.btn.btn-primary.submit-button").length === 1;
        });
    }, function() {
        test.assertExists("button.btn.btn-primary.submit-button");
        this.click("button.btn.btn-primary.submit-button");
    }, function() {
        test.fail('Failed to locate the Submit button');
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll("a.btn.btn-primary").length === 1;
       });
    }, function() {
        test.assertExists("a.btn.btn-primary");
        this.click("a.btn.btn-primary");
    }, function() {
        test.fail('Failed to locate the Add/Upgrade button');
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll("button.btn.btn-default.cancel-button").length === 1;
        });
    }, function() {
        test.assertExists("button.btn.btn-default.cancel-button");
        this.click("button.btn.btn-default.cancel-button");
    }, function() {
        test.fail('Failed to locate the Cancel button');
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll("a.btn.btn-primary").length === 1;
        });
    }, function() {
        test.assertExists("a.btn.btn-primary");
        this.click("a.btn.btn-primary");
    }, function() {
        test.fail('Failed to locate the Add/Upgrade button');
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll("button.close").length === 1;
        });
    }, function() {
        test.assertExists("button.close");
        this.click("button.close");
    }, function() {
        test.fail('Failed to locate the Close/X button');
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll("a.btn.btn-primary").length === 1;
        });
    }, function() {
        test.assertExists("a.btn.btn-primary");
        this.click("a.btn.btn-primary");
    }, function() {
        test.fail('Failed to locate the Add/Upgrade button');
    });

//Check Maven URL functionality
    casper.then(function() {
        test.comment("Checking Maven URL Upload functionality");
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll("#url").length === 1;
        });
    }, function() {
        test.assertExists("#url");
        this.click("#url");
    }, function() {
        test.fail('Failed to locate the Add/Upgrade button');
    });

    casper.then(function () {
        this.evaluate(function() {
            $('input#urlField').val('blah');
        });
    });

    casper.thenClick('#add-url-btn');

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

    casper.thenClick('#add-url-btn');

    casper.waitFor(function() {
        return this.evaluate(function() {
            return $('span.file-fail-text').html() === '';
        });
    }, function() {
        test.pass('Entered a valid Maven URL');
    }, function() {
        test.fail('Entered an invalid Maven URL');
    });

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('li.url-list-item').length === 1;
        });
    }, function() {
        test.pass('List of Maven URLs has entries');
    }, function() {
        test.fail('List of Maven URLs is empty');
    });

    casper.thenClick('a.remove-url-link.glyphicon.glyphicon-remove')

    casper.waitFor(function() {
        return this.evaluate(function() {
            return document.querySelectorAll('li.url-list-item').length === 0;
        });
    }, function() {
        test.pass('List of Maven URLs is empty');
    }, function() {
        test.fail('List of Maven URLs has entries');
    });

    casper.then(function() {
        test.comment("Checking File Upload functionality");
    });

    casper.waitForSelector("#upload",
        function success() {
            this.click("#upload");
            test.pass("Clicked the File Upload tab");
        },
        function fail() {
            test.fail("Failed to locate the File Upload tab");
    });

    casper.waitForSelector("#app-upload-btn",
        function success() {
            this.click("#app-upload-btn");
            test.pass("Clicked the \'Add application files...\' button");
        },
        function fail() {
            test.fail("Failed to locate the \'Add application files...\' button");
    });

    casper.waitForSelector(".btn.btn-default.cancel-button",
        function success() {
            this.click(".btn.btn-default.cancel-button");
            test.pass("Clicked the Cancel button");
        },
        function fail() {
            test.fail("Failed to locate the Cancel button");
    });

    casper.run(function() {test.done();});
});
