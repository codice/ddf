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

define(['backbone', 'jquery'], function(Backbone, $) {
  var QueryMonitor = {}

  QueryMonitor.MonitorModel = Backbone.Model.extend({
    initialize: function() {
      this.set({ users: [] })
      this.pollActiveSearches()
    },
    getActiveSearches: function() {
      var url =
        '../jolokia/exec/org.codice.ddf.resourcemanagement.query.service.QueryMonitor:service=querymonitor/activeSearches/'
      var that = this
      $.ajax({
        url: url,
        dataType: 'json',
        success: function(data) {
          that.set({ users: data.value })
        },
      })
    },
    stopSearch: function(uuid) {
      var url =
        '../jolokia/exec/org.codice.ddf.resourcemanagement.query.service.QueryMonitor:service=querymonitor/cancelActiveSearch/' +
        uuid
      $.ajax({
        url: url,
        dataType: 'json',
      })
    },
    pollActiveSearches: function() {
      var that = this
      ;(function poll() {
        setTimeout(function() {
          that.getActiveSearches()
          poll()
        }, 1000)
      })()
    },
  })

  return QueryMonitor
})
