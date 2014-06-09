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
 **//* global require */

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

            //if we have passed in data, we are assuming that we want to setup a channel to listen
            //this means we don't want to listen to a response from the service endpoint
            if(options.data) {
                model.subscription = Cometd.Comet.subscribe('/' + guid, options.success);
                options.data.guid = guid;
            } else { //just listen for a response from the service endpoint
                model.subscription = Cometd.Comet.subscribe(model.url, options.success);
            }

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