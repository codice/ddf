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
define(['underscore', 'backbone'], function(_, Backbone) {
  var AlertsModel = {}

  AlertsModel.Alert = Backbone.Model.extend({
    defaults: {
      'details-button-action': 'Show',
      collapse: 'out',
      level: 'danger',
    },
  })

  AlertsModel.BackendAlerts = Backbone.Collection.extend({
    model: AlertsModel.Alert,
    url:
      './jolokia/read/org.codice.ddf.ui.admin.api:type=AdminAlertMBean/Alerts',
    parse: function(response) {
      return response.value
    },
  })

  AlertsModel.JolokiaAlerts = AlertsModel.Alert.extend({
    defaults: _.extend({}, AlertsModel.Alert.prototype.defaults, {
      title: 'Unable to save your changes.',
    }),
    parse: function(response) {
      if (response.stacktrace) {
        var json = {}

        var stackLines = response.stacktrace.split(/\n/)
        json.details = []
        for (var i = 0; i < stackLines.length; i++) {
          json.details.push({ message: stackLines[i] })
        }

        if (response.stacktrace === 'Forbidden') {
          json.details = [
            {
              message:
                'An error was received while trying to contact the server, which indicates that you might no longer be logged in.',
            },
          ]
          json.title =
            'Your session has expired. Please <a href="/login/index.html?prevurl=/admin/">log in</a> again.'
        }
        return json
      } else {
        return response
      }
    },
  })

  AlertsModel.Jolokia = function(jolokiaResponse) {
    return new AlertsModel.JolokiaAlerts(jolokiaResponse, { parse: true })
  }

  return AlertsModel
})
