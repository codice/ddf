/* global require */

(function (undefined) {
    "use strict";
    var _ = require('underscore'),
        $ = require('jquery'),
        Backbone = require('backbone'),
        Cometd = require('cometdinit');

    var cometdUnbind = function () {
        Cometd.Comet.unsubscribe(this.subscription);
    };

    Backbone.Model.prototype.origDestroy = Backbone.Model.prototype.destroy;
    Backbone.Collection.prototype.origDestroy = Backbone.Collection.prototype.destroy;
    var destroyModel = function (options) {
        cometdUnbind();
        Backbone.Model.prototype.origDestroy(options);
    };
    var destroyColl = function (options) {
        cometdUnbind();
        Backbone.Collection.prototype.origDestroy(options);
    };
    Backbone.Model.prototype.destroy = destroyModel;
    Backbone.Collection.prototype.destroy = destroyColl;

    var origSync = Backbone.sync;

    var generateGuid = function (model) {
        var guid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            var r = Math.random() * 16 | 0,
                v = c == 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
        model.guid = guid;
        return guid;
    };

    Backbone.Model.prototype.mergeLatest = function () {
        if(this.lastResponse) {
            return this.set(this.parse(this.lastResponse));
        }
    };

    Backbone.sync = function (method, model, options) {
        if (options.useAjaxSync || model.useAjaxSync) {
            return origSync(method, model, options);
        } else {
            var deferred = $.Deferred();
            //create a primary key for this object if we don't have one already
            var guid = model.guid || generateGuid(model);

            //method doesn't really matter
            if (model.subscription) {
                Cometd.Comet.unsubscribe(model.subscription);
            }
            var success = options.success;
            options.success = function (resp) {
                if (typeof options.progress == 'function') {
                    options.progress(1, resp);
                }
                if(!model.lastResponse) {
                    var retVal = success(resp);
                    if (retVal === false) {
                        deferred.reject();
                    } else {
                        deferred.resolve();
                        model.lastResponse = resp;
                    }
                } else {
                    model.lastResponse = resp;
                }
            };
            model.subscription = Cometd.Comet.subscribe('/' + guid, options.success);

            options.data.guid = guid;

            Cometd.Comet.publish(model.url, options.data);

            var promise = deferred.promise();
            promise.complete = promise.done;
            promise.success = promise.done;
            promise.error = promise.fail;

            model.trigger('request', model, promise, options);
            return promise;
        }
    };

}(undefined));