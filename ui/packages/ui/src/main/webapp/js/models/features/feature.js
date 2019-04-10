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

define(['backbone', 'jquery', 'underscore'], function(Backbone, $, _) {
  var Feature = {}

  var featureUrl =
    './jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/AllFeatures'
  var installUrl =
    './jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/installFeature(java.lang.String)/'
  var uninstallUrl =
    './jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/uninstallFeature(java.lang.String)/'

  Feature.Model = Backbone.Model.extend({
    initialize: function(options) {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
      this.name = options.name
    },
    urlRoot: function() {
      return featureUrl
    },
    install: function() {
      return $.ajax({
        type: 'GET',
        url: installUrl + this.name,
        dataType: 'JSON',
      })
    },
    uninstall: function() {
      return $.ajax({
        type: 'GET',
        url: uninstallUrl + this.name,
        dataType: 'JSON',
      })
    },
  })

  Feature.Collection = Backbone.Collection.extend({
    model: Feature.Model,
    comparator: 'name',
    url: function() {
      return featureUrl
    },
    parse: function(resp) {
      return resp.value.map(function(blob) {
        blob.id = blob.name
        return blob
      })
    },
  })

  return Feature
})
