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
  'q',
  'js/model/Service.js',
  'backbone',
  'underscore',
  'backboneassociation',
], function(Q, Service, Backbone, _) {
  var Registry = {}

  Registry.ConfigurationList = Backbone.Collection.extend({
    model: Service.Configuration,
  })

  Registry.Model = Backbone.AssociatedModel.extend({
    configUrl:
      '../../jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui',
    initialize: function() {
      this.set('registryConfiguration', new Registry.ConfigurationList())
    },
    addRegistryConfiguration: function(registry) {
      this.get('registryConfiguration').add(registry)
    },
    removeRegistry: function(registry) {
      this.get('registryConfiguration').remove(registry)
    },
    size: function() {
      return this.get('registryConfiguration').length
    },
  })

  Registry.Collection = Backbone.Collection.extend({
    model: Registry.Model,
    addRegistry: function(configuration) {
      var registry = new Registry.Model(configuration.get('properties'))
      registry.addRegistryConfiguration(configuration)

      this.add(registry)
      registry.trigger('change')
    },
    removeRegistry: function(registry) {
      this.remove(registry)
    },
    comparator: function(model) {
      var str = model.get('id') || ''
      return str.toLowerCase()
    },
  })

  Registry.Response = Backbone.Model.extend({
    initialize: function(options) {
      if (options.model) {
        this.model = options.model
        var collection = new Registry.Collection()
        this.set({ collection: collection })
        this.listenTo(this.model, 'change', this.parseServiceModel)
      }
    },
    parseServiceModel: function() {
      var resModel = this
      var collection = resModel.get('collection')
      collection.reset()
      if (this.model.get('value')) {
        this.model.get('value').each(function(service) {
          if (!_.isEmpty(service.get('configurations'))) {
            service.get('configurations').each(function(configuration) {
              collection.addRegistry(configuration)
            })
          }
        })
      }
      collection.sort()
      collection.trigger('reset')
    },
    getRegistryMetatypes: function() {
      var resModel = this
      var metatypes = []
      if (resModel.model.get('value')) {
        resModel.model.get('value').each(function(service) {
          var id = service.get('id')
          var name = service.get('name')
          if (this.isRegistryName(id) || this.isRegistryName(name)) {
            metatypes.push(service)
          }
        })
      }
      return metatypes
    },
    isRegistryName: function(val) {
      return val && val.indexOf('Registry') !== -1
    },
    getRegistryModel: function(initialModel) {
      var resModel = this
      var serviceCollection = resModel.model.get('value')
      if (!initialModel) {
        initialModel = new Registry.Model()
      }

      if (serviceCollection) {
        serviceCollection.each(function(service) {
          var config = new Service.Configuration({ service: service })
          config.set('fpid', config.get('fpid'))
          initialModel.addRegistryConfiguration(config)
        })
      }
      return initialModel
    },
    createDeletePromise: function(registry, config) {
      var deferred = Q.defer()
      var serviceModels = this.model.get('value')
      config
        .destroy()
        .done(function() {
          //sync up the service model so that the refresh updates properly
          serviceModels.remove(config.getService())
          deferred.resolve({
            registry: registry,
            config: config,
          })
        })
        .fail(function() {
          deferred.reject(
            new Error(
              "Unable to delete configuration '" + registry.get('name') + "'."
            )
          )
        })
      return deferred.promise
    },
  })

  return Registry
})
