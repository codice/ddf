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
  'backbone.marionette',
  'js/views/module/ModuleDetail.layout',
  'js/models/module/ModulePlugin',
  'js/models/AppConfigPlugin',
  'components/tab-content/tab-content.collection.view',
  'components/tab-item/tab-item.collection.view',
  'q',
], function(
  Backbone,
  Marionette,
  ModuleDetailLayout,
  ModulePlugin,
  AppConfigPlugin,
  PluginTabContentView,
  PluginTabView,
  Q
) {
  var ModuleDetailController = Marionette.Controller.extend({
    initialize: function(options) {
      this.regions = options.regions
    },
    show: function() {
      var layoutView = new ModuleDetailLayout()
      this.regions.applications.show(layoutView)

      this.fetchSystemConfigPlugins()
        .then(function(systemConfigPlugins) {
          var staticModulePlugins = [
            new Backbone.Model({
              id: 'systemInformationModuleTabID',
              displayName: 'Information',
              javascriptLocation:
                'components/system-information/system-information.view.js',
            }),
            new Backbone.Model({
              id: 'featureModuleTabID',
              displayName: 'Features',
              javascriptLocation: 'components/features/features.view.js',
            }),
            new Backbone.Model({
              id: 'configurationModuleTabID',
              displayName: 'Configuration',
              javascriptLocation:
                'components/application-services/application-services.view',
            }),
          ]

          var staticList = new ModulePlugin.Collection()
          staticList.comparator = function(model) {
            return model.get('displayName')
          }
          staticList.add(staticModulePlugins)

          var dynamicList = new Backbone.Collection()
          dynamicList.comparator = function(model) {
            return model.get('displayName')
          }
          dynamicList.add(systemConfigPlugins.models)
          dynamicList.sort()

          var completeList = new Backbone.Collection()
          completeList.add(staticList.models)
          completeList.add(dynamicList.models)

          layoutView.tabs.show(new PluginTabView({ collection: completeList }))
          layoutView.tabContent.show(
            new PluginTabContentView({ collection: completeList })
          )
          layoutView.selectFirstTab()
        })
        .fail(function(error) {
          throw error
        })
    },
    fetchSystemConfigPlugins: function() {
      var pageName = 'system-module'
      var collection = new AppConfigPlugin.Collection()
      var defer = Q.defer()
      collection.fetchByAppName(pageName, {
        success: function() {
          defer.resolve(collection)
        },
        failure: function() {
          defer.reject(new Error('Error fetching system page plugins for {0}'))
        },
      })
      return defer.promise
    },
  })

  return ModuleDetailController
})
