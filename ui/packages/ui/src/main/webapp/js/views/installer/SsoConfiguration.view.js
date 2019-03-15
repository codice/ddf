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
define([
  'backbone.marionette',
  'underscore',
  'js/wreqr.js',
  'jquery',
  'templates/installer/ssoConfiguration.handlebars',
  'templates/installer/ssoConfigurationTable.handlebars',
  'views/installer/SsoConfigSimple.view.js',
  'views/installer/SsoConfigBoolean.view.js',
  'views/installer/SsoConfigOptions.view.js',
  'views/installer/SsoConfigMultiple.view.js',
], function(
  Marionette,
  _,
  wreqr,
  $,
  viewTemplate,
  tableTemplate,
  SsoConfigSimple,
  SsoConfigBoolean,
  SsoConfigOptions,
  SsoConfigMultiple
) {
  var IDP_CLIENT_METATYPE_ID = 'org.codice.ddf.security.idp.client.IdpMetadata'
  var IDP_SERVER_METATYPE_ID = 'org.codice.ddf.security.idp.server.IdpEndpoint'
  var OIDC_HANDLER_METATYPE_ID =
    'org.codice.ddf.security.handler.api.OidcHandlerConfiguration'

  var STRING_TYPE = 1
  var INTEGER_TYPE = 3
  var BOOLEAN_TYPE = 11

  function getTypeNameFromType(type) {
    switch (type) {
      case STRING_TYPE:
        return 'String'
      case INTEGER_TYPE:
        return 'Integer'
      case BOOLEAN_TYPE:
        return 'Boolean'
    }
  }

  /* Displays different metatypes as SsoConfigurationCategories and allows switching between categories */
  var SsoConfigurationView = Marionette.Layout.extend({
    template: viewTemplate,
    className: 'full-height sso-config-view',
    regions: {
      oidcRegion: '#oidc-category',
      samlRegion: '#saml-category',
    },
    events: {
      'click #saml-tab': 'showSaml',
      'click #oidc-tab': 'showOidc',
    },
    initialize: function(options) {
      this.metatypes = options.metatypes
      this.navigationModel = options.navigationModel
      this.navigationModel.set('hidePrevious', false)
      this.modified = false
      this.listenTo(this.navigationModel, 'next', this.next)
      this.listenTo(this.navigationModel, 'previous', this.previous)
      this.listenTo(wreqr.vent, 'ssoConfigModified', this.setModified)

      this.samlMetatypes = []
      this.oidcMetatypes = []
      this.sortMetatypes()

      this.samlCategory = new SsoConfigurationCategory({
        metatypes: this.samlMetatypes,
      })
      this.oidcCategory = new SsoConfigurationCategory({
        metatypes: this.oidcMetatypes,
      })
    },
    sortMetatypes: function() {
      var self = this

      _.each(self.metatypes, function(metatype) {
        switch (metatype.get('id')) {
          case IDP_CLIENT_METATYPE_ID:
          case IDP_SERVER_METATYPE_ID:
            self.samlMetatypes.push(metatype)
            break
          case OIDC_HANDLER_METATYPE_ID:
            self.oidcMetatypes.push(metatype)
            break
          default:
            break
        }
      })
    },
    onRender: function() {
      this.samlTab = this.$('#saml-tab')
      this.oidcTab = this.$('#oidc-tab')

      this.samlRegion.show(this.samlCategory)
      this.oidcRegion.show(this.oidcCategory)

      this.showSaml()
    },
    showSaml: function(event) {
      this.samlTab.attr('isSelected', 'true')
      this.oidcTab.attr('isSelected', 'false')

      this.oidcRegion.$el.hide()
      this.samlRegion.$el.show()
    },
    showOidc: function(event) {
      this.samlTab.attr('isSelected', 'false')
      this.oidcTab.attr('isSelected', 'true')

      this.samlRegion.$el.hide()
      this.oidcRegion.$el.show()
    },
    setModified: function() {
      this.modified = true
    },
    next: function(event) {
      if (this.hasErrors()) {
        this.navigationModel.nextStep(
          'There is an error in one or more field(s). Please correct and try again.',
          0
        )
        return
      }

      this.persistConfig()

      this.navigationModel.set('modified', this.modified)
      this.navigationModel.nextStep('', 100)
    },
    previous: function() {
      this.navigationModel.previousStep()
    },
    persistConfig: function() {
      var config = this.getConfig()

      var data = {
        type: 'WRITE',
        mbean:
          'org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0',
        attribute: 'SsoConfigurations',
        value: config,
      }

      $.ajax({
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(data),
        url:
          './jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0',
        success: function() {
          wreqr.vent.trigger('ssoConfigPersisted')
        },
      })
    },
    getConfig: function() {
      var result = []

      _.each(this.samlCategory.getConfig(), function(config) {
        result.push(config)
      })

      _.each(this.oidcCategory.getConfig(), function(config) {
        result.push(config)
      })

      return result
    },
    hasErrors: function() {
      return this.samlCategory.hasErrors() || this.oidcCategory.hasErrors()
    },
    onClose: function() {
      this.stopListening(this.navigationModel)
    },
  })

  /* Displays multiple metatypes as SsoConfigurationTables */
  var SsoConfigurationCategory = Marionette.Layout.extend({
    //template: false,
    tagName: 'div',
    initialize: function(options) {
      this.metatypes = options.metatypes

      this.tables = []
      this.initTables()
    },
    initTables: function() {
      var self = this

      _.each(self.metatypes, function(metatype) {
        var table = new SsoConfigurationTable({
          metatype: metatype,
        })

        self.tables.push(table)
      })
    },
    onRender: function() {
      this.rootElement = this.el

      this.renderTables()
    },
    renderTables: function() {
      var self = this

      _.each(self.tables, function(table) {
        table.render()
        self.el.append(table.el)
      })
    },
    getConfig: function() {
      var config = []

      _.each(this.tables, function(table) {
        config.push(table.getConfig())
      })

      return config
    },
    hasErrors: function() {
      var hasErrors = false

      _.each(this.tables, function(table) {
        if (table.hasErrors()) {
          hasErrors = true
        }
      })

      return hasErrors
    },
  })

  /* Displays all the different metatype entries (Simple, Boolean, Options, Multiple) in a metatype */
  var SsoConfigurationTable = Marionette.Layout.extend({
    template: tableTemplate,
    tagName: 'div',
    className: 'sso-config-table-container',
    initialize: function(options) {
      this.metatype = options.metatype
      this.metatypeName = this.metatype.get('name')
      this.metatypeId = this.metatype.get('id')

      this.metatypeEntries = []
    },
    serializeData: function() {
      return {
        metatypeName: this.metatypeName,
        metatypeId: this.metatypeId,
      }
    },
    onRender: function() {
      this.tableBody = this.$el.find('.table-body')

      this.populateTable()
    },
    populateTable: function() {
      // gathers the values for the metatype entries
      var metatypeValues =
        (
          (
            (
              ((this.metatype.attributes.configurations || {}).models ||
                [])[0] || {}
            ).attributes || {}
          ).properties || {}
        ).attributes || {}

      var self = this

      _.each(self.metatype.get('metatype').models, function(metatypeEntry) {
        var tableEntry = self.createTableEntry(metatypeEntry, metatypeValues)

        tableEntry.render()

        self.metatypeEntries.push(tableEntry)

        self.tableBody.append(tableEntry.el)
      })
    },
    createTableEntry: function(metatypeEntry, metatypeValues) {
      var name = metatypeEntry.get('name')
      var id = metatypeEntry.get('id')
      var value = metatypeValues[id]
      var defaultValue = metatypeEntry.get('defaultValue')
      var description = metatypeEntry.get('description')
      var options = metatypeEntry.get('options')
      var cardinality = metatypeEntry.get('cardinality')
      var type = metatypeEntry.get('type')
      var typeName = getTypeNameFromType(type)

      var entryInfo = {
        name: name,
        value: value,
        defaultValue: defaultValue,
        description: description,
        id: id,
        options: options,
        cardinality: cardinality,
        type: type,
        typeName: typeName,
      }

      if (!_.isEmpty(options)) {
        return new SsoConfigOptions(entryInfo)
      }

      if (type === BOOLEAN_TYPE) {
        return new SsoConfigBoolean(entryInfo)
      }

      if (cardinality !== 0) {
        return new SsoConfigMultiple(entryInfo)
      }

      return new SsoConfigSimple(entryInfo)
    },
    getConfig: function() {
      var config = {
        metatypeName: this.metatypeName,
        metatypeId: this.metatypeId,
        metatypeEntries: [],
      }
      _.each(this.metatypeEntries, function(metatypeEntry) {
        config.metatypeEntries.push(metatypeEntry.getConfig())
      })

      return config
    },
    hasErrors: function() {
      var hasErrors = false

      _.each(this.metatypeEntries, function(metatypeEntry) {
        if (metatypeEntry.hasErrors()) {
          hasErrors = true
        }
      })

      return hasErrors
    },
  })

  return SsoConfigurationView
})
