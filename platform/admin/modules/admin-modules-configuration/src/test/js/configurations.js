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
var x = require('casper').selectXPath;

casper.test.begin('Configurations View test', function(test) {
    casper.start('http://localhost:8383');

    casper.waitForSelector('#configurations',
    function success() {
        test.pass('Found configurations container');
    },
    function fail() {
        test.fail('Did NOT find configurations container');
    });

    casper.waitForSelector('#servicesRegion',
    function success() {
        test.pass('Found servicesRegion container');
    },
    function fail() {
        test.fail('Did NOT find servicesRegion container');
    });

    casper.waitForSelector('#ddfplatformconfig',
    function success() {
        test.pass('Found ddfplatformconfig container');
    },
    function fail() {
        test.fail('Did NOT find ddfplatformconfig container');
    });

    casper.waitForSelector(x("//a[normalize-space(text())='Platform Global Configuration']"),
        function success() {
            test.assertExists(x("//a[normalize-space(text())='Platform Global Configuration']"));
            this.click(x("//a[normalize-space(text())='Platform Global Configuration']"));
        },
        function fail() {
            test.assertExists(x("//a[normalize-space(text())='Platform Global Configuration']"));
    });
    casper.waitForSelector(".btn.btn-default.cancel-button",
        function success() {
            test.assertExists(".btn.btn-default.cancel-button");
            this.click(".btn.btn-default.cancel-button");
        },
        function fail() {
            test.assertExists(".btn.btn-default.cancel-button");
    });
    casper.waitForSelector(x("//a[normalize-space(text())='ddf.platform.config']"),
        function success() {
            test.assertExists(x("//a[normalize-space(text())='ddf.platform.config']"));
            this.click(x("//a[normalize-space(text())='ddf.platform.config']"));
        },
        function fail() {
            test.assertExists(x("//a[normalize-space(text())='ddf.platform.config']"));
    });

    casper.waitForSelector("#ddfplatformconfig form.add-federated-source",
        function success() {
            test.assertExists("#ddfplatformconfig form.add-federated-source");
            test.comment('Entering values for Platform Global Configuration');
        },
        function fail() {
            test.assertExists("#ddfplatformconfig form.add-federated-source");
        }
    );

    casper.then(function () {
        this.fill('#ddfplatformconfig .add-federated-source', { protocol: "http" }, false);
        test.comment('Entered value for protocol');
    });

    casper.then(function () {
        this.fill('#ddfplatformconfig .add-federated-source', { host: "localhost" }, false);
        test.comment('Entered value for host');
    });
    casper.then(function () {
        this.fill('#ddfplatformconfig .add-federated-source', { port: "8181" }, false);
        test.comment('Entered value for port');
    });

    casper.then(function () {
        this.fill('#ddfplatformconfig .add-federated-source', { trustStore: "" }, false);
        test.comment('Entered value for trustStore');
    });

    casper.then(function () {
        this.fill('#ddfplatformconfig .add-federated-source', { trustStorePassword: "" }, false);
        test.comment('Entered value for trustStorePassword');
    });

    casper.then(function () {
        this.fill('#ddfplatformconfig .add-federated-source', { keyStore: "" }, false);
        test.comment('Entered value for keyStore');
    });

    casper.then(function () {
        this.fill('#ddfplatformconfig .add-federated-source', { keyStorePassword: "" }, false);
        test.comment('Entered value for keyStorePassword');
    });

    casper.then(function () {
        this.fill('#ddfplatformconfig .add-federated-source', { id: "ddf.distribution" }, false);
        test.comment('Entered value for id');
    });

    casper.then(function () {
        this.fill('#ddfplatformconfig .add-federated-source', { version: "2.3.0" }, false);
        test.comment('Entered value for version');
    });

    casper.then(function () {
        this.fill('#ddfplatformconfig .add-federated-source', { organization: "Codice Foundation" }, false);
        test.comment('Entered value for organization');
    });

    casper.waitForSelector(".btn.btn-primary.submit-button",
        function success() {
            test.assertExists(".btn.btn-primary.submit-button");
            this.click(".btn.btn-primary.submit-button");
        },
        function fail() {
            test.assertExists(".btn.btn-primary.submit-button");
    });

    casper.run(function() {test.done();});
});
