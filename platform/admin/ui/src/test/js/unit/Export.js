/*jslint node: true */
/* global describe, it, require */
'use strict';

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
var expect = require('chai').expect;
var sinon = require('sinon');
var Squire = requirejs('squirejs');
var document = require('jsdom').jsdom();
var window = document.defaultView;
require('jquery')(window);
var $ = window.$;
var dependencyInjector = new Squire();
var dependencyInjectedRequire = dependencyInjector.mock('jquery', $);
var ajaxStub = sinon.stub($, 'ajax');

var tryAssertions = require('../shared/utility.js').tryAssertions;

var baseResponse;
var exportModelPath = 'src/main/webapp/js/models/module/Export';

beforeEach(function () {
    baseResponse = {
        value: [],
        status: 200,
        error: undefined
    }
});

describe('Export', function () {
    describe('#export', function () {

        it('should set errors if there are any with a 200 status', function (done) {
            var deferred = $.Deferred();
            baseResponse.error = "error";
            deferred.resolve(baseResponse);
            ajaxStub.returns(deferred);
            dependencyInjectedRequire.require([exportModelPath], function (Export) {
                var exportModel = new Export();
                exportModel.on('change:inProgress', function (model, inProgress) {
                    if (!inProgress) {
                        done(tryAssertions(function () {
                            expect(exportModel.get('errors').length).to.equal(1);
                        }));
                    }
                });
                exportModel.export();
            });
        });

        it('should set errors if there is anything other than a 200 status', function (done) {
            var deferred = $.Deferred();
            baseResponse.status = 403;
            deferred.reject(baseResponse);
            ajaxStub.returns(deferred);
            dependencyInjectedRequire.require([exportModelPath], function (Export) {
                var exportModel = new Export();
                exportModel.on('change:inProgress', function (model, inProgress) {
                    if (!inProgress) {
                        done(tryAssertions(function () {
                            expect(exportModel.get('errors').length).to.equal(1);
                        }));
                    }
                });
                exportModel.export();
            });
        });

        it('should set appropriate error message if anything other than a 200 status', function (done) {
            var deferred = $.Deferred();
            baseResponse.status = 404;
            baseResponse.statusText = 'error';
            deferred.reject(baseResponse);
            ajaxStub.returns(deferred);
            dependencyInjectedRequire.require([exportModelPath], function (Export) {
                var exportModel = new Export();
                exportModel.on('change:inProgress', function (model, inProgress) {
                    if (!inProgress) {
                        done(tryAssertions(function () {
                            expect(exportModel.get('errors')[0]).to.equal('404: error');
                        }));
                    }
                });
                exportModel.export();
            });
        });

        it('should set warnings if there are any with a 200 status', function (done) {
            var deferred = $.Deferred();
            baseResponse.value.push({message: "warning"});
            deferred.resolve(baseResponse);
            ajaxStub.returns(deferred);
            dependencyInjectedRequire.require([exportModelPath], function (Export) {
                var exportModel = new Export();
                exportModel.on('change:inProgress', function (model, inProgress) {
                    if (!inProgress) {
                        done(tryAssertions(function () {
                            expect(exportModel.get('warnings').length).to.equal(1);
                        }));
                    }
                });
                exportModel.export();
            });
        });

        it('should clear out existing warnings', function (done) {
            var deferred = $.Deferred();
            ajaxStub.returns(deferred);
            dependencyInjectedRequire.require([exportModelPath], function (Export) {
                var exportModel = new Export();
                exportModel.set('warnings', ['warning']);
                exportModel.export();
                done(tryAssertions(function () {
                    expect(exportModel.get('warnings').length).to.equal(0);
                }));
            });
        });

        it('should clear out existing errors', function (done) {
            var deferred = $.Deferred();
            ajaxStub.returns(deferred);
            dependencyInjectedRequire.require([exportModelPath], function (Export) {
                var exportModel = new Export();
                exportModel.set('errors', ['error']);
                exportModel.export();
                done(tryAssertions(function () {
                    expect(exportModel.get('errors').length).to.equal(0);
                }));
            });
        });

        it('should set inProgress to true during export', function (done) {
            var deferred = $.Deferred();
            ajaxStub.returns(deferred);
            dependencyInjectedRequire.require([exportModelPath], function (Export) {
                var exportModel = new Export();
                exportModel.export();
                done(tryAssertions(function () {
                    expect(exportModel.get('inProgress')).to.equal(true);
                }));
            });
        });

        it('should set inProgress to false after export', function (done) {
            var deferred = $.Deferred();
            ajaxStub.returns(deferred);
            deferred.resolve(baseResponse);
            dependencyInjectedRequire.require([exportModelPath], function (Export) {
                var exportModel = new Export();
                exportModel.export();
                // jQuery 3+ Deferred, for Promises/A+ compliance, no longer executes callbacks
                // synchronously when added to an already resolved/rejected promise, so we need
                // to set a timeout before declaring our expectations here.
                setTimeout(function(){
                    done(tryAssertions(function () {
                        expect(exportModel.get('inProgress')).to.equal(false);
                    }));
                },0);
            });
        });

        it('should set inProgress to false after export even if there are errors', function (done) {
            var deferred = $.Deferred();
            ajaxStub.returns(deferred);
            baseResponse.status = 403;
            deferred.reject(baseResponse);
            dependencyInjectedRequire.require([exportModelPath], function (Export) {
                var exportModel = new Export();
                exportModel.export();
                setTimeout(function(){
                    done(tryAssertions(function () {
                        expect(exportModel.get('inProgress')).to.equal(false);
                    }));
                },0);
            });
        });

        it('adding an error should emit a change event', function (done) {
            var changeEmitted = false;
            var deferred = $.Deferred();
            ajaxStub.returns(deferred);
            baseResponse.status = 404;
            baseResponse.statusText = 'error';
            deferred.reject(baseResponse);
            dependencyInjectedRequire.require([exportModelPath], function (Export) {
                var exportModel = new Export();
                exportModel.on("change:errors", function () {
                    changeEmitted = true;
                });
                exportModel.export();
                setTimeout(function(){
                    done(tryAssertions(function () {
                        expect(changeEmitted).to.equal(true);
                    }));
                },0);
            });
        });

    });
});