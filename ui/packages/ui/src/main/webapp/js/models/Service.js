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
  'backbone',
  'jquery',
  'js/ServiceFactoryNameRegistry',
  'js/MetatypeRegistry',
  'backboneassociations',
], function(Backbone, $, ServiceFactoryNameRegistry, MetatypeRegistry) {
  function isServiceFactory(properties) {
    return properties.get('service.factoryPid')
  }

  Backbone.Associations.SEPARATOR = '~'

  var Service = {}

  Service.Metatype = Backbone.AssociatedModel.extend({
    initialize: function() {
      this.transformOptions()
    },
    transformOptions: function() {
      var optionLabels = this.get('optionLabels')
      var optionValues = this.get('optionValues')
      if (optionValues) {
        this.set(
          'options',
          optionValues.reduce(function(blob, value, index) {
            blob[value] = {
              label: optionLabels[index],
              value: value,
            }
            return blob
          }, {})
        )
      }
    },
  })

  Service.Properties = Backbone.AssociatedModel.extend({})

  Service.Configuration = Backbone.AssociatedModel.extend({
    configUrl:
      './jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0',

    defaults: function() {
      return {
        properties: new Service.Properties(),
      }
    },

    relations: [
      {
        type: Backbone.One,
        key: 'properties',
        relatedModel: Service.Properties,
        includeInJSON: true,
      },
    ],

    initialize: function(options) {
      if (options && options.properties && options.properties['service.pid']) {
        this.set({
          uuid: options.properties['service.pid'].replace(/\./g, ''),
        })
      }
      this.set('displayName', this.getConfigurationDisplayName())
    },

    /**
     * Collect all the data to save.
     * @param pid The pid id.
     * @returns {{type: string, mbean: string, operation: string}}
     */
    collectedData: function(pid) {
      var model = this
      var data = {
        type: 'EXEC',
        mbean:
          'org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0',
        operation: 'update',
      }
      data.arguments = [pid]
      data.arguments.push(model.get('properties').toJSON())
      return data
    },

    /**
     * Get the serviceFactoryPid PID
     * @param model, this is really this model.
     * @returns an ajax promise
     */
    makeConfigCall: function(model) {
      if (!model) {
        return
      }
      var configUrl = [
        model.configUrl,
        'createFactoryConfiguration',
        model.get('fpid'),
      ].join('/')
      return $.ajax({
        type: 'GET',
        url: configUrl,
      })
    },

    /**
     * Since jolokia always returns a 200, we need to parse the response
     * to determine whether the request succeeded or failed, and trigger
     * the corresponding callback
     */
    handleResult: function(result, deferred) {
      // per ajax api, should be three args with the first
      // either a string or a jqxhr object
      if (
        !(
          result.length === 3 &&
          (typeof result[0] === 'string' || result[0].setRequestHeader)
        )
      ) {
        throw 'Unexpected result contents'
      }
      var jsonResult
      var jsonString = result[0]
      if (typeof jsonString === 'string') {
        try {
          jsonResult = JSON.parse(jsonString.toString().trim())
        } catch (e) {
          // https://codice.atlassian.net/browse/DDF-1642
          // this works around an issue in json-simple where the .toString() of an array
          // is returned in the arguments field of configs with array attributes,
          // causing the JSON string from jolokia to be unparseable, so we remove it,
          // since we don't care about the arguments for our parsing needs
          jsonString = jsonString.replace(/\[L[\w\.;@]*/g, '""')
          jsonResult = JSON.parse(jsonString.toString().trim())
        }
      } else {
        jsonResult = { error: result[1], stacktrace: result[2] }
      }
      if (typeof jsonResult.error === 'undefined') {
        deferred.resolve.apply(null, result)
      } else {
        deferred.reject.call(null, result[2], 'fail', jsonResult)
      }
    },

    /**
     * When a model calls save the sync is called in Backbone.  I override it because this isn't a typical backbone
     * object
     * @return Return a deferred which is a handler with the success and failure callback.
     */
    sync: function() {
      var deferred = $.Deferred(),
        model = this,
        addUrl = [model.configUrl, 'add'].join('/')
      //if it has a pid we are editing an existing record
      var configExists = model.get('id')
      var isFactory = model.get('fpid')
      if (configExists || !isFactory) {
        // config exists OR is a non-factory config.  non-factory configs do not needs to be "makeConfigCall"-ed
        var collect = model.collectedData(
          model.get('properties').get('service.pid') ||
            model.parents[0].get('id')
        )
        var jData = JSON.stringify(collect)

        $.ajax({
          type: 'POST',
          contentType: 'application/json',
          data: jData,
          url: addUrl,
        }).always(function() {
          model.handleResult(arguments, deferred)
        })
      } else {
        //no pid means this is a new record
        model.makeConfigCall(model).done(function(data) {
          var collect = model.collectedData(JSON.parse(data).value)
          var jData = JSON.stringify(collect)

          $.ajax({
            type: 'POST',
            contentType: 'application/json',
            data: jData,
            url: addUrl,
          }).always(function() {
            model.handleResult(arguments, deferred)
          })
        })
      }
      return deferred
    },
    destroy: function() {
      var deleteUrl = [
        this.configUrl,
        'delete',
        this.get('properties').get('service.pid'),
      ].join('/')

      return $.ajax({
        type: 'GET',
        url: deleteUrl,
      })
    },
    initializeFromModel: function(model) {
      if (model.get('factory')) {
        return this.initializeFromMSF(model)
      } else {
        return this.initializeFromService(model)
      }
    },
    initializeFromMSF: function(msf) {
      var fpid = msf.get('id')
      this.set({
        fpid: fpid,
        name: msf.get('name'),
      })
      this.get('properties').set({ 'service.factoryPid': fpid })
      return this.initializeFromService(msf)
    },
    initializeFromService: function(service) {
      return this.initializeFromMetatype(service.get('metatype'))
    },
    initializeFromMetatype: function(metatype) {
      this.get('properties').set(
        metatype.reduce(function(defaults, obj) {
          // Check for existence of default value;
          // If it doesn't exist, and it should be a boolean (type: 11), then set it to "false"
          var defaultVal = obj.get('defaultValue')
          if (defaultVal) {
            defaults[obj.get('id')] = defaultVal
          } else if (obj.get('type') === 11) {
            defaults[obj.get('id')] = 'false'
          } else {
            defaults[obj.get('id')] = null
          }
          return defaults
        }, {})
      )
      return this
    },
    getConfigurationDisplayName: function() {
      var displayName = this.get('id')
      var properties = this.get('properties')
      if (isServiceFactory(properties)) {
        displayName =
          ServiceFactoryNameRegistry.getName(properties) ||
          properties.get('name') ||
          properties.get('shortname') ||
          properties.get('id') ||
          displayName
      } else if (displayName === undefined && properties !== undefined) {
        displayName = properties.get('service.pid')
      } else if (displayName === undefined) {
        displayName = JSON.stringify(this.toJSON())
      }
      return displayName
    },
  })

  Service.ConfigurationList = Backbone.Collection.extend({
    model: Service.Configuration,
    comparator: function(model) {
      return model.getConfigurationDisplayName().toLowerCase()
    },
  })

  Service.Model = Backbone.AssociatedModel.extend({
    configUrl:
      './jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0',

    defaults: function() {
      return {
        configurations: new Service.ConfigurationList(),
      }
    },

    relations: [
      {
        type: Backbone.Many,
        key: 'configurations',
        relatedModel: Service.Configuration,
        collectionType: Service.ConfigurationList,
        includeInJSON: true,
      },
      {
        type: Backbone.Many,
        key: 'metatype',
        relatedModel: Service.Metatype,
        collectionType: Service.MetatypeList,
        includeInJSON: false,
      },
    ],

    initialize: function(options) {
      if (options && options.id) {
        this.set({ uuid: options.id.replace(/\./g, '') })
      }
      MetatypeRegistry.add(this.id, this.get('metatype'))
    },

    hasConfiguration: function() {
      if (this.get('configurations')) {
        return true
      }
      return false
    },
    parse(data) {
      /**
       * The backend leaves off empty configurations, so our collection won't update.
       * We can work around this by setting the configurations to empty if it's left off.
       */
      data.configurations = data.configurations || []
      return data
    },
  })

  Service.Response = Backbone.AssociatedModel.extend({
    defaults: function() {
      return {
        request: undefined,
        status: undefined,
        timestamp: undefined,
        value: [],
        fetched: false,
      }
    },
    relations: [
      {
        type: Backbone.Many,
        key: 'value',
        relatedModel: Service.Model,
        collectionType: Backbone.Collection.extend({
          model: Service.Model,
          comparator: function(model) {
            return model.get('name')
          },
        }),
        includeInJSON: false,
      },
    ],

    initialize: function(options) {
      this.listenTo(
        this,
        'sync',
        function() {
          this.set('fetched', true)
        }.bind(this)
      )
      if (options && options.url) {
        this.url = options.url
      } else {
        this.url =
          './jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/listServices'
      }
    },
  })

  return Service
})
