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

define(['backbone', 'jquery', 'underscore'], function(Backbone, $, _) {
  'use strict'

  const Applications = {}

  const versionRegex = /([^0-9]*)([0-9]+.*$)/

  // Applications.TreeNode
  // ---------------------

  // Represents a node in the application tree where children are dependent on their parent being
  // installed. This node can have zero or more children (which are also 'Applications.Treenode`
  // nodes themselves).
  Applications.TreeNode = Backbone.Model.extend({
    defaults: function() {
      return {
        chosenApp: false,
        selected: false,
      }
    },

    initialize: function() {
      const children = this.get('children')
      const that = this
      const changeObj = {}

      // Some (not properly created) applications features file result in a name that includes the
      // version number - strip that off and move it into the version number.
      this.massageVersionNumbers()
      this.cleanupDisplayName()
      this.updateName()
      this.updateDescription()

      // Reflect the current state of the application in the model and keep the
      // state to determine if the user changes it.
      changeObj.selected = changeObj.currentState =
        this.get('state') === 'ACTIVE'
      changeObj.chosenApp = false
      changeObj.error = false

      // Change the children from json representation to models and include a link
      // in each to their parent.
      if (children) {
        changeObj.children = new Applications.TreeNodeCollection(children)
        this.set(changeObj)
        this.get('children').forEach(function(child) {
          child.set({ parent: that })
        })
      } else {
        this.set(changeObj)
      }
    },

    // Since the name is used for ids in the DOM, remove any periods
    // that might exist - but store in a separate attribute since we need the
    // original name to control the application via the application-service.
    updateName: function() {
      this.set({ appId: this.get('name').replace(/\./g, '') })
    },

    // Some apps come in having the version number included
    // as part of the app name - e.g. search-app-2.3.1.ALPHA3-SNAPSHOT.
    // This function strips the version from the display name and
    // places it in the version variable so the details show correctly.
    massageVersionNumbers: function() {
      const changeObj = {}
      changeObj.displayName = this.get('name')
      if (this.get('version') === '0.0.0') {
        const matches = this.get('name').match(versionRegex)
        if (matches.length === 3) {
          changeObj.displayName = matches[1]
          changeObj.version = matches[2]
        }
      }
      this.set(changeObj)
    },

    // Create a name suitable for display. First attempts to parse a display name from
    // the description (expecting a form of "application description::display name". If that
    // doesn't yield a display name, then it extracts it from the application name - camel-case
    // it and remove the dashes.
    cleanupDisplayName: function() {
      const changeObj = {}

      if (this.has('description')) {
        const desc = this.get('description')
        const values = desc.split('::')
        if (values.length > 1) {
          changeObj.description = values[0]
          changeObj.displayName = values[1]
        }
      }

      if (typeof changeObj.displayName === 'undefined') {
        const tempName = this.get('displayName') //.replace(/\./g,'');
        const names = tempName.split('-')
        let workingName = ''
        const that = this
        _.each(names, function(name) {
          if (workingName.length > 0) {
            workingName = workingName + ' '
          }
          workingName = workingName + that.capitalizeFirstLetter(name)
        })
        changeObj.displayName = workingName
      }

      this.set(changeObj)
    },

    // Capitalize and return the first letter of the given string.
    capitalizeFirstLetter: function(string) {
      if (string && string !== '') {
        return string.charAt(0).toUpperCase() + string.slice(1)
      }
      return string
    },

    // If the description has multiple paragraphs (separated by new line characters), build
    // an array of each paragraph for the details template to display.
    updateDescription: function() {
      if (this.has('description')) {
        const descArray = this.get('description').split('\\n')
        this.set('paragraphs', descArray)
      }
    },
  })

  // Applications.TreeNodeCollection
  // -------------------------------

  // Represents a collection of application nodes. Note that each of the `Applications.Treenode`
  // elements can be recursive nodes.
  Applications.TreeNodeCollection = Backbone.Collection.extend({
    model: Applications.TreeNode,
    url:
      './jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/Applications/',

    comparator: function(model) {
      return model.get('displayName')
    },
  })

  // Applications.Response
  // ---------------------

  // Represents the response from the application-service when obtaining the list of all applications
  // on the system.
  let applicationsResponseCache
  Applications.Response = Backbone.Model.extend({
    fetch: function() {
      if (applicationsResponseCache === undefined) {
        applicationsResponseCache = $.ajax({
          type: 'GET',
          url:
            './jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/Applications/',
          dataType: 'JSON',
        }).then(function(appsResp) {
          return appsResp.value
        })
      }
      return applicationsResponseCache
    },
  })

  return Applications
})
