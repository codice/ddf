/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/* global define */
define([
    'backbone',
    'q',
    'underscore'
],function (Backbone, Q, _) {
    /**
     * A very simple promise wrapper of fetch that just resolves or rejects.  Warning! If
     * you define a success or error handler in options, this will overwrite them for now.
     *
     * @param options
     * @returns {Q Promise}
     */
    var fetchPromise = function(options){
        var deferred = Q.defer(),
            modelOrCollection = this;
        options = options ? _.clone(options) : {};

        options.success = function(){
            deferred.resolve.apply(deferred,arguments);
        };
        options.error = function(){
            deferred.reject.apply(deferred,arguments);
        };
        modelOrCollection.fetch(options);
        return deferred.promise;

    };

    if (typeof Backbone.Collection.prototype.fetchPromise !== 'function'){
        Backbone.Collection.prototype.fetchPromise = fetchPromise;
    }
    if (typeof Backbone.Model.prototype.fetchPromise !== 'function'){
        Backbone.Model.prototype.fetchPromise = fetchPromise;
    }
});