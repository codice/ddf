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
  'marionette',
  'underscore',
  'jquery',
  './query-interactions.hbs',
  'js/CustomElements',
  'js/store',
  'component/lightbox/lightbox.view.instance',
  'component/query-feedback/query-feedback.view',
  'component/confirmation/query/confirmation.query.view',
  'component/loading/loading.view',
  'component/query-annotations/query-annotations.view',
  'properties',
], function(
  wreqr,
  Marionette,
  _,
  $,
  template,
  CustomElements,
  store,
  lightboxInstance,
  QueryFeedbackView,
  QueryConfirmationView,
  LoadingView,
  QueryAnnotationsView,
  properties
) {
  return Marionette.ItemView.extend({
    template: template,
    tagName: CustomElements.register('query-interactions'),
    className: 'composed-menu',
    modelEvents: {},
    events: {
      'click .interaction-run': 'handleRun',
      'click .interaction-refresh-result-count': 'handleRefreshResultCount',
      'click .interaction-stop': 'handleCancel',
      'click .interaction-delete': 'handleDelete',
      'click .interaction-duplicate': 'handleDuplicate',
      'click .interaction-deleted': 'handleDeleted',
      'click .interaction-historic': 'handleHistoric',
      'click .interaction-feedback': 'handleFeedback',
      'click .interaction-annotations': 'handleAnnotations',
      click: 'handleClick',
    },
    ui: {},
    initialize: function() {
      if (!this.model.get('result')) {
        this.startListeningToSearch()
      }
      this.handleResult()
      QueryConfirmationView = require('component/confirmation/query/confirmation.query.view')
      this.$el.toggleClass(
        'is-archive-searchable',
        !properties.isArchiveSearchEnabled()
      )
      this.$el.toggleClass(
        'is-history-searchable',
        !properties.isHistoricalSearchEnabled()
      )
      this.$el.toggleClass(
        'is-versioning-enabled',
        !properties.isVersioningEnabled
      )
    },
    onRender: function() {
      this.handleLocal()
    },
    handleLocal: function() {
      this.$el.toggleClass('is-local', this.model.isLocal())
    },
    startListeningToSearch: function() {
      this.listenToOnce(
        this.model,
        'change:result',
        this.startListeningForResult
      )
    },
    startListeningForResult: function() {
      this.listenToOnce(
        this.model.get('result'),
        'sync error',
        this.handleResult
      )
    },
    handleRun: function() {
      this.model.startSearchFromFirstPage()
    },
    handleRefreshResultCount: function() {
      this.model.startSearch({ resultCountOnly: true })
    },
    handleCancel: function() {
      this.model.cancelCurrentSearches()
    },
    handleDelete: function() {
      this.model.collection.remove(this.model)
    },
    handleDeleted: function() {
      this.model.startSearch({
        limitToDeleted: true,
      })
    },
    handleDuplicate: function() {
      var copyAttributes = JSON.parse(JSON.stringify(this.model.attributes))
      delete copyAttributes.id
      delete copyAttributes.result
      var newQuery = new this.model.constructor(copyAttributes)
      if (this.model.collection.canAddQuery()) {
        this.model.collection.add(newQuery)
        store.setCurrentQuery(newQuery)
      } else {
        this.listenTo(
          QueryConfirmationView.generateConfirmation({}),
          'change:choice',
          function(confirmation) {
            var choice = confirmation.get('choice')
            if (choice === true) {
              var loadingview = new LoadingView()
              store
                .get('workspaces')
                .once('sync', function(workspace, resp, options) {
                  loadingview.remove()
                  wreqr.vent.trigger('router:navigate', {
                    fragment: 'workspaces/' + workspace.id,
                    options: {
                      trigger: true,
                    },
                  })
                })
              store.get('workspaces').createWorkspaceWithQuery(newQuery)
            } else if (choice !== false) {
              store.getCurrentQueries().remove(choice)
              store.getCurrentQueries().add(newQuery)
              store.setCurrentQuery(newQuery)
            }
          }.bind(this)
        )
      }
    },
    handleHistoric: function() {
      this.model.startSearch({
        limitToHistoric: true,
      })
    },
    handleFeedback: function() {
      lightboxInstance.model.updateTitle('Search Quality Feedback')
      lightboxInstance.model.open()
      lightboxInstance.lightboxContent.show(
        new QueryFeedbackView({
          model: this.model,
        })
      )
    },
    handleAnnotations: function() {
      lightboxInstance.model.updateTitle('Search Notes')
      lightboxInstance.model.open()
      lightboxInstance.lightboxContent.show(
        new QueryAnnotationsView({
          model: this.model,
        })
      )
    },
    handleResult: function() {
      this.$el.toggleClass(
        'has-results',
        this.model.get('result') !== undefined
      )
    },
    handleClick: function() {
      this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    },
  })
})
