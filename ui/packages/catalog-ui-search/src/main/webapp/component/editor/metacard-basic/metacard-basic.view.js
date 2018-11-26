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
/*global define, setTimeout*/
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
  className: 'is-metacard-basic',
  setDefaultModel: function() {
    this.model = this.selectionInterface.getSelectedResults()
  },
  selectionInterface: store,
  initialize: function(options) {
    this.selectionInterface =
      options.selectionInterface || this.selectionInterface
    EditorView.prototype.initialize.call(this, options)
    this.listenTo(
      this.model
        .first()
        .get('metacard')
        .get('properties'),
      'change',
      this.onBeforeShow
    )
  },
  onBeforeShow: function() {
    this.editorProperties.show(
      PropertyCollectionView.generateSummaryPropertyCollectionView([
        this.model
          .first()
          .get('metacard>properties')
          .toJSON(),
      ])
    )
    this.editorProperties.currentView.$el.addClass('is-list')
    this.getValidation()
    EditorView.prototype.onBeforeShow.call(this)
  },
  getValidation: function() {
    if (!this.model.first().isRemote()) {
      var self = this
      self.editorProperties.currentView.clearValidation()
      $.get({
        url:
          './internal/metacard/' +
          this.model.first().get('metacard').id +
          '/attribute/validation',
        customErrorHandling: true,
      }).then(function(response) {
        if (!self.isDestroyed && self.editorProperties.currentView) {
          self.editorProperties.currentView.updateValidation(response)
        }
      })
    }
  },
  afterCancel: function() {},
  afterSave: function(editorJSON) {
    if (editorJSON.length > 0) {
      var payload = [
        {
          ids: [
            this.model
              .first()
              .get('metacard')
              .get('id'),
          ],
          attributes: editorJSON,
        },
      ]
      LoadingCompanionView.beginLoading(this)
      var self = this
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
                self.getValidation()
              }
            }, 1000)
          })
      }, 1000)
    }
  },
})
