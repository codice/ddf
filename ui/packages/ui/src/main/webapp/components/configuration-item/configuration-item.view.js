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
/*global define, window*/
define([
  'underscore',
  'marionette',
  'components/configuration-edit/configuration-edit.view',
  'js/wreqr.js',
  './configuration-item.hbs',
  'js/CustomElements',
], function(
  _,
  Marionette,
  ConfigurationEdit,
  wreqr,
  configurationRow,
  CustomElements
) {
  return Marionette.Layout.extend({
    template: configurationRow,
    tagName: CustomElements.register('configuration-item'),
    events: {
      'click .editLink': 'editConfiguration',
      'click .removeLink': 'removeConfiguration',
    },
    regions: {
      editModal: '.config-modal',
    },
    editConfiguration: function() {
      wreqr.vent.trigger('poller:stop')
      this.editModal.show(
        new ConfigurationEdit.View({
          model: this.model,
          service: this.model.collection.parents[0],
        })
      )
      wreqr.vent.trigger('refresh')
    },
    removeConfiguration: function() {
      var question =
        'Are you sure you want to remove the configuration: ' +
        this.model.get('properties').get('service.pid') +
        '?'
      var confirmation = window.confirm(question)
      if (confirmation) {
        var configuration = this
        configuration.model.destroy().done(function() {
          wreqr.vent.trigger('refreshConfigurations')
          configuration.close()
        })
      }
    },
    serializeData: function() {
      return _.extend(
        this.model.toJSON(),
        { displayName: this.model.getConfigurationDisplayName() },
        {
          hasFactory: this.model.get('fpid'),
          cid: this.cid,
        }
      )
    },
  })
})
