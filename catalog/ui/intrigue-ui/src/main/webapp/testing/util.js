/*jslint node: true */
/* global require*/
module.exports = {
    /*
     Mocha Utility for Asynchronous Tests
     If done is not called in asynchonous tests, mocha simply reports a timeout error.
     However, we're more concerned with the assertion that threw the error than the
     timeout.  This wrapper function ensures that done will be called with the appropriate
     error (the assertion error), resulting in better test reports.
     */
    tryAssertions: function (assertions) {
        try {
            assertions();
        } catch (error) {
            return error;
        }
    },
    getSquire: function () {
        var requirejs = require('requirejs');
        requirejs.config({
            baseUrl: '.',
            nodeRequire: require,
            packages: [
                {
                    name: 'squirejs',
                    location: 'node_modules/squirejs',
                    main: 'src/Squire'
                }
            ]
        });
        return requirejs('squirejs');
    },
    getJquery: function () {
        var document = require('jsdom').jsdom();
        var window = document.defaultView;
        require('jquery')(window);
        return window.$;
    },
    getBackbone: function ($) {
        $ = $ || this.getJquery();
        var Backbone = require('backbone');
        Backbone.$ = $;
        return Backbone;
    },
    mockAjax: function ($) {
        var deferred = new $.Deferred();
        var ajaxMock = {
            destroy: function () {
                $.ajax.restore();
                this.destroy = $.noop;
            },
            resetDeferred: function () {
                if (deferred.state() === 'pending')
                    deferred.resolve();
                deferred = new $.Deferred();
                return this;
            },
            resolveDeferred: function (response, success) {
                if (deferred.state() !== 'pending')
                    this.resetDeferred();
                deferred.resolve(response, success);
            }
        };
        require('sinon').stub($, 'ajax', function (options) {
            return deferred.always(function (response, success) {
                if (success) {
                    options.success(response); // needed for backbone fetch to work
                } else {
                    options.error(response);  // needed for backbone fetch to work
                }
                return response;  // for typical ajax calls
            });
        });
        return ajaxMock;
    }
};