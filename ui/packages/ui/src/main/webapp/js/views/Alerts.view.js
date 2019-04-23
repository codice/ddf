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

define([
  'backbone.marionette',
  'templates/alerts.handlebars',
  'jquery',
  'underscore',
], function(Marionette, alertsTemplate, $, _) {
  const AlertsView = {};

  const dismissUrl =
    './jolokia/exec/org.codice.ddf.ui.admin.api:type=AdminAlertMBean/dismissAlert';

  AlertsView.View = Marionette.ItemView.extend({
    template: alertsTemplate,
    events: {
      'shown.bs.collapse': 'toggleDetailsMsg',
      'hidden.bs.collapse': 'toggleDetailsMsg',
      'click .dismiss': 'dismissAlert',
    },
    modelEvents: {
      change: 'render',
    },
    toggleDetailsMsg: function() {
      const model = this.model;
      model.get('details-button-action') === 'Show'
        ? model.set('details-button-action', 'Hide')
        : model.set('details-button-action', 'Show')
      model.get('collapse') === 'out'
        ? model.set('collapse', 'in')
        : model.set('collapse', 'out')
    },
    serializeData: function() {
      const json = this.model.toJSON();
      json.showChevron = true
      json.details = _.map(json.details, function(val) {
        if (val.message !== undefined) {
          json.showChevron = false
          return val.message
        }
        return val
      })
      json.collapseId =
        'alertCollapse_' + parseInt(Math.random() * Math.pow(2, 32), 10)
      switch (this.model.get('priority')) {
        case 1:
        case '1':
        case 2:
        case '2':
          json.level = 'info'
          break
        case 3:
        case '3':
          json.level = 'warning'
          break
        case 4:
        case '4':
          json.level = 'danger'
          break
        default:
          json.level = 'danger'
          break
      }
      return json
    },
    // Performs the actual AJAX call to dismiss the alert
    dismissAlert: function() {
      const model = this.model;

      let data = {
        type: 'EXEC',
        mbean: 'org.codice.ddf.ui.admin.api:type=AdminAlertMBean',
        operation: 'dismissAlert',
      };
      data.arguments = [model.get('id')]
      data = JSON.stringify(data)
      $.ajax({
        type: 'POST',
        contentType: 'application/json',
        data: data,
        url: dismissUrl,
      }).done(function() {
        model.trigger('destroy', model)
      })
    },
  })

  return AlertsView
})
