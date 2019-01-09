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
/*global define*/
define([
  'wreqr',
  'js/model/Service.js',
  'backbone',
  'underscore',
  'jquery',
  'q',
  'poller',
  'js/model/Status.js',
  'backboneassociation',
], function(wreqr, Service, Backbone, _, $, Q, poller, Status) {
  var Source = {}

  Source.ConfigurationList = Backbone.Collection.extend({
    model: Service.Configuration,
  })
  Source.Model = Backbone.Model.extend({
    configUrl:
      '../jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui',
    idAttribute: 'name',
    defaults: {
      currentUrl: undefined,
      isLoopbackUrl: undefined,
    },
    initialize: function() {
      this.set('currentConfiguration', undefined)
      this.set('disabledConfigurations', new Source.ConfigurationList())
      this.listenTo(
        this,
        'change:currentConfiguration',
        this.updateCurrentBinding
      )
      this.listenTo(this, 'remove', this.onRemove)
      this.updateCurrentBinding()
    },
    onRemove: function() {
      this.stopListening()
      this.stopPolling()
    },
    stopPolling: function() {
      var statusModel = this.get('statusModel')
      if (statusModel) {
        statusModel.stopListening()
        poller.get(statusModel).destroy()
      }
    },
    addDisabledConfiguration: function(configuration) {
      if (
        this.get('disabledConfigurations') &&
        !this.get('disabledConfigurations').findWhere({
          fpid: configuration.get('fpid'),
        })
      ) {
        this.get('disabledConfigurations').add(configuration)
      }
    },
    removeConfiguration: function(configuration) {
      if (this.get('disabledConfigurations').contains(configuration)) {
        this.stopListening(configuration)
        this.get('disabledConfigurations').remove(configuration)
      } else if (configuration === this.get('currentConfiguration')) {
        this.stopListening(configuration)
        this.stopPolling()
        this.set('currentConfiguration', undefined)
      }
    },
    setCurrentConfiguration: function(configuration) {
      var sync = 'sync'
      var statusUpdate = 'status:update-'
      this.set({ currentConfiguration: configuration })

      var pid = configuration.id

      var statusModel = this.get('statusModel')
      if (statusModel) {
        statusModel.set({ id: pid })
      } else {
        statusModel = new Status.Model({ id: pid })
        statusModel.on(sync, function() {
          wreqr.vent.trigger(statusUpdate + pid, statusModel)
        })
        this.set('statusModel', statusModel)
      }

      poller.get(statusModel, { delay: 30000 }).start()
    },
    hasConfiguration: function(configuration) {
      var id = configuration.get('id')
      var curConfig = this.get('currentConfiguration')
      var hasConfig = false

      var found = this.get('disabledConfigurations').find(function(config) {
        return config.get('fpid') === id + '_disabled'
      })
      if (_.isUndefined(found)) {
        if (!_.isUndefined(curConfig)) {
          hasConfig = curConfig.get('fpid') === id
        }
      } else {
        hasConfig = true
      }
      return hasConfig
    },
    initializeFromMSF: function(msf) {
      this.set({
        fpid: msf.get('id'),
      })
      this.set({
        name: msf.get('name'),
      })
      this.initializeConfigurationFromMetatype(msf.get('metatype'))
      this.configuration.set({
        'service.factoryPid': msf.get('id'),
      })
    },
    initializeConfigurationFromMetatype: function(metatype) {
      var src = this
      src.configuration = new Source.Configuration()
      metatype.forEach(function(obj) {
        var id = obj.id
        var val = obj.defaultValue
        src.configuration.set(id, val ? val.toString() : null)
      })
    },
    size: function() {
      var ct = _.isUndefined(this.get('currentConfiguration')) ? 0 : 1
      return ct + this.get('disabledConfigurations').length
    },
    getActions: function(action) {
      var src = this
      var currentConfig = src.get('currentConfiguration')
      var actions = []

      if (!_.isUndefined(currentConfig)) {
        actions = currentConfig.get(action)
      }
      src.get('disabledConfigurations').forEach(function(config) {
        if (config.get(action)) {
          config.get(action).forEach(function(actionItem) {
            src.addUnique(actions, actionItem)
          })
        }
      })

      return actions
    },
    getCurrentUrl: function() {
      var src = this
      var currentConfig = src.get('currentConfiguration')
      if (currentConfig !== undefined) {
        var configProps = currentConfig.attributes.properties.attributes
        var configPropKeys = Object.keys(configProps)

        var urls = configPropKeys.filter(function(item) {
          return /.*Address|.*Url/.test(item)
        })
        var filteredKeys = urls.filter(function(item) {
          return /^(?!event|site).*$/.test(item)
        })

        return configProps[filteredKeys]
      }
      return undefined
    },
    checkLoopback: function() {
      if (this.get('currentUrl') === undefined) {
        return false
      }
      return /.*(localhost|org.codice.ddf.system.hostname).*/.test(
        this.getCurrentUrl().toString()
      )
    },
    updateCurrentBinding: function() {
      this.set('currentUrl', this.getCurrentUrl())
      this.set('isLoopbackUrl', this.checkLoopback())
    },
    addUnique: function(uniqueArray, addThis) {
      if (_.isUndefined(addThis)) {
        return
      }

      var unique = true
      uniqueArray.forEach(function(arrayItem) {
        if (_.isEqual(arrayItem, addThis)) {
          unique = false
        }
      })

      if (unique) {
        uniqueArray.push(addThis)
      }
    },
    /**
     *  Method to get a list of all configs, each with its corresponding service and properties
     */
    getAllConfigsWithServices: function() {
      var theConfigs = this.getAllConfigServices()
      var listOfConfigStrings = []
      theConfigs.models.forEach(
        function(con) {
          listOfConfigStrings.push(con.id)
        }.bind(this)
      )
      var configsWithServices = []
      listOfConfigStrings.forEach(
        function(conString) {
          configsWithServices.push(this.findConfigFromId(conString))
        }.bind(this)
      )
      return configsWithServices
    },
    /**
     * Uses the current context's model to return a Backbone collection of all configurations service's
     */
    getAllConfigServices: function() {
      var configs = new Backbone.Collection()
      var disabledConfigs = this.get('disabledConfigurations')
      var currentConfig = this.get('currentConfiguration')
      if (!_.isUndefined(currentConfig)) {
        var currentService = currentConfig.get('service')
        configs.add(currentService)
      }
      if (!_.isUndefined(disabledConfigs)) {
        disabledConfigs.each(function(config) {
          configs.add(config.get('service'))
        })
      }
      return configs
    },
    /**
     *  Retrieve a configuration with its service by its string id.
     */
    findConfigFromId: function(id) {
      var currentConfig = this.get('currentConfiguration')
      var disabledConfigs = this.get('disabledConfigurations')
      var config
      if (!_.isUndefined(currentConfig) && currentConfig.get('fpid') === id) {
        config = currentConfig
      } else {
        if (!_.isUndefined(disabledConfigs)) {
          config = disabledConfigs.find(function(item) {
            var service = item.get('service')
            if (!_.isUndefined(service) && !_.isNull(service)) {
              return service.get('id') === id
            }
            return false
          })
        }
      }

      return config
    },
    /**
     * Perform the action provider action
     */
    performAction: function(actionId, url) {
      return $.ajax({
        url: url,
        type: this.getHttpMethod(actionId),
      })
    },
    /**
     * @param id
     *   the action provider id that contains the HTTP method in it
     * @returns the HTTP method parsed from the id, GET is the default return if one isn't found
     */
    getHttpMethod: function(id) {
      var httpIndex = id.indexOf('HTTP_')
      if (httpIndex > 0) {
        return id.substring(httpIndex + 5)
      }
      return 'GET'
    },
  })

  Source.Collection = Backbone.Collection.extend({
    model: Source.Model,
    addSource: function(configuration, enabled) {
      var source
      var sourceId = configuration.get('properties').get('shortname')
      if (!sourceId) {
        sourceId = configuration.get('properties').get('id')
      }
      if (this.get(sourceId)) {
        source = this.get(sourceId)
      } else {
        source = new Source.Model({
          name: sourceId,
        })
        this.add(source)
      }
      if (enabled) {
        source.setCurrentConfiguration(configuration)
      } else {
        source.addDisabledConfiguration(configuration)
      }
      source.trigger('change')
    },
    removeSource: function(source) {
      this.remove(source)
    },
    removeAllSources: function() {
      var source
      while ((source = this.first())) {
        this.removeSource(source)
      }
    },
    comparator: function(model) {
      var str = model.get('name') || ''
      return str.toLowerCase()
    },
  })

  Source.Response = Backbone.Model.extend({
    initialize: function(options) {
      if (options.model) {
        this.model = options.model
        var collection = new Source.Collection()
        this.set({
          collection: collection,
        })
        this.listenTo(this.model, 'change', this.parseServiceModel)
      }
    },
    parseServiceModel: function() {
      var resModel = this
      var collection = resModel.get('collection')
      collection.removeAllSources()
      if (this.model.get('value')) {
        this.model.get('value').each(function(service) {
          if (!_.isEmpty(service.get('configurations'))) {
            service.get('configurations').each(function(configuration) {
              var cfgService = configuration.get('service')
              if (
                configuration.get('id') &&
                (resModel.isSourceName(configuration.get('fpid')) ||
                  (cfgService && resModel.isSourceName(cfgService.get('name'))))
              ) {
                if (configuration.get('fpid').indexOf('_disabled') === -1) {
                  collection.addSource(configuration, true)
                } else {
                  collection.addSource(configuration, false)
                }
              }
            })
          }
        })
      }
      collection.sort()
      collection.trigger('reset')
    },
    getSourceMetatypes: function() {
      var resModel = this
      var metatypes = []
      if (resModel.model.get('value')) {
        resModel.model.get('value').each(function(service) {
          var id = service.get('id')
          var name = service.get('name')
          if (this.isSourceName(id) || this.isSourceName(name)) {
            metatypes.push(service)
          }
        })
      }
      return metatypes
    },
    isSourceName: function(val) {
      return val && val.indexOf('Source') !== -1
    },
    /**
     * Returns a SourceModel that has all available source type configurations. Each source type configuration will be added as a
     * disabledConfiguration and returned as part of the model. If an initialModel is presented, it will be modified to include any
     * missing configurations as part of its disabledConfigurations.
     */
    getSourceModelWithServices: function(initialModel) {
      var resModel = this
      var serviceCollection = resModel.model.get('value')
      if (!initialModel) {
        initialModel = new Source.Model()
      }
      if (serviceCollection) {
        serviceCollection.each(function(service) {
          var config = new Service.Configuration({
            service: service,
          })
          config.set('fpid', config.get('fpid') + '_disabled')
          initialModel.addDisabledConfiguration(config)
        })
      }
      return initialModel
    },
    isSourceConfiguration: function(configuration) {
      return (
        configuration.get('fpid') &&
        configuration.get('id') &&
        configuration.get('fpid').indexOf('Source') !== -1
      )
    },
    createDeletePromise: function(source, config) {
      var deferred = Q.defer()
      var serviceModels = this.get('model').get('value')
      config
        .destroy()
        .done(function() {
          source.removeConfiguration(config)
          //sync up the service model so that the refresh updates properly
          serviceModels.remove(config.getService())
          deferred.resolve({
            source: source,
            config: config,
          })
        })
        .fail(function() {
          deferred.reject(
            new Error(
              "Unable to delete configuration '" + source.get('name') + "'."
            )
          )
        })
      return deferred.promise
    },
  })
  return Source
})
