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
  'icanhaz',
  'underscore',
  'marionette',
  'js/models/Service',
  'components/configuration-edit/configuration-edit.view',
  'js/wreqr.js',
  './service-item.hbs',
  'js/CustomElements',
  'components/configuration-item/configuration-item.collection.view',
], function(
  ich,
  _,
  Marionette,
  Service,
  ConfigurationEdit,
  wreqr,
  serviceRow,
  CustomElements,
  ConfigurationItemCollectionView
) {
  ich.addTemplate('serviceRow', serviceRow)

  return Marionette.Layout.extend({
    template: 'serviceRow',
    tagName: CustomElements.register('service-item'),
    events: {
      'click .newLink': 'newConfiguration',
    },
    regions: {
      collectionRegion: '#configurationRegion',
      editModal: '.service-modal',
    },
    onRender: function() {
      this.collectionRegion.show(
        new ConfigurationItemCollectionView({
          collection: this.model.get('configurations'),
        })
      )
    },
    /**
     * If it is a factory OR it has no existing configurations, generate a configuration from the model.
     * Otherwise, use the existing configuration.
     * Then show an editing modal with the configuration.
     */
    newConfiguration: function() {
      var configuration
      var model = this.model
      var hasFactory = model.get('factory')
      var existingConfigurations = model.get('configurations')

      if (hasFactory || existingConfigurations.isEmpty()) {
        wreqr.vent.trigger('poller:stop')
        configuration = new Service.Configuration().initializeFromModel(model)
      } else {
        configuration = existingConfigurations.at(0)
      }

      this.editModal.show(
        new ConfigurationEdit.View({ model: configuration, service: model })
      )
    },

    serializeData: function() {
      return _.extend(this.model.toJSON(), {
        cid: this.cid,
      })
    },
  })
})
