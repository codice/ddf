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
const Backbone = require('backbone')
const Q = require('q')
const _ = require('underscore')
require('backbone-associations')
// Backbone associations uses "." as its standard sub-object selecting within their framework.
// However since some of our json attribute names have "." characters in the name, this causes
// associations to do undesired sub-object querying when we do simple set operations on models
// model.set("my.attribute.name","foo"). If we ever want utilize this pathing, we need to use
// ">" instead so we don't conflicts with the pathing functionality.
//
// if someone wants to use the Backbone.Association sub-object selecting, they can do
// model.get('object>subObject>deeperSubObject');
//
// This sub object selecting can be see at
// http://dhruvaray.github.io/backbone-associations/specify-associations.html#sa-getsetop
Backbone.Associations.setSeparator('>')

/**
 * A very simple promise wrapper of fetch that just resolves or rejects.  Warning! If
 * you define a success or error handler in options, this will overwrite them for now.
 *
 * @param options
 * @returns {Q Promise}
 */
const fetchPromise = function(options) {
  const deferred = Q.defer(),
    modelOrCollection = this
  options = options ? _.clone(options) : {}

  options.success = function() {
    deferred.resolve.apply(deferred, arguments)
  }
  options.error = function() {
    deferred.reject.apply(deferred, arguments)
  }
  modelOrCollection.fetch(options)
  return deferred.promise
}

if (typeof Backbone.Collection.prototype.fetchPromise !== 'function') {
  Backbone.Collection.prototype.fetchPromise = fetchPromise
}
if (typeof Backbone.Model.prototype.fetchPromise !== 'function') {
  Backbone.Model.prototype.fetchPromise = fetchPromise
}
