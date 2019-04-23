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

const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const EditorView = require('../editor.view')
const store = require('../../../js/store.js')
const PropertyCollectionView = require('../../property/property.collection.view.js')
const LoadingCompanionView = require('../../loading-companion/loading-companion.view.js')
const alertInstance = require('../../alert/alert.js')
const metacardDefinitions = require('../../singletons/metacard-definitions.js')
const ResultUtils = require('../../../js/ResultUtils.js')

module.exports = EditorView.extend({
  className: 'is-metacards-basic',
  setDefaultModel: function() {
    this.model = this.selectionInterface.getSelectedResults()
  },
  selectionInterface: store,
  initialize: function(options) {
    this.selectionInterface =
      options.selectionInterface || this.selectionInterface
    EditorView.prototype.initialize.call(this, options)
  },
  onBeforeShow: function() {
    const results = this.selectionInterface.getSelectedResults()
    const metacards = results.map(function(result) {
      return result.get('metacard>properties').toJSON()
    })
    this.editorProperties.show(
      PropertyCollectionView.generatePropertyCollectionView(metacards)
    )
    this.editorProperties.currentView.$el.addClass('is-list')
    this.getValidation()
    EditorView.prototype.onBeforeShow.call(this)
  },
  getEditorActionsOptions: function() {
    return {
      summary: false,
    }
  },
  getValidation: function() {
    const results = this.selectionInterface.getSelectedResults()
    const self = this
    self.editorProperties.currentView.clearValidation()
    results
      .filter(function(result) {
        return !result.isRemote()
      })
      .forEach(function(result) {
        ;(function(id) {
          $.get({
            url: './internal/metacard/' + id + '/attribute/validation',
            customErrorHandling: true,
          }).then(function(response) {
            if (!self.isDestroyed && self.editorProperties.currentView) {
              response.forEach(function(issue) {
                issue.id = id
              })
              self.editorProperties.currentView.updateValidation(response)
            }
          })
        })(
          result
            .get('metacard')
            .get('properties')
            .get('id')
        )
      })
  },
  afterCancel: function() {
    //this.getValidation();
  },
  afterSave: function(editorJSON) {
    if (editorJSON.length > 0) {
      const payload = [
        {
          ids: this.model.map(function(metacard) {
            return metacard.get('metacard').get('id')
          }),
          attributes: editorJSON,
        },
      ]
      LoadingCompanionView.beginLoading(this)
      const self = this
      setTimeout(function() {
        $.ajax({
          url: './internal/metacards',
          type: 'PATCH',
          data: JSON.stringify(payload),
          contentType: 'application/json',
        })
          .then(function(response) {
            ResultUtils.updateResults(self.model, response)
          })
          .always(function() {
            setTimeout(function() {
              //let solr flush
              LoadingCompanionView.endLoading(self)
              if (!self.isDestroyed) {
                self.onBeforeShow()
              }
            }, 1000)
          })
      }, 1000)
    }
  },
})
