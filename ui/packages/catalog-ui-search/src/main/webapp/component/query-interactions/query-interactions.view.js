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
const wreqr = require('../../js/wreqr.js')
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./query-interactions.hbs')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const lightboxInstance = require('../lightbox/lightbox.view.instance.js')
const QueryFeedbackView = require('../query-feedback/query-feedback.view.js')
const QueryConfirmationView = require('../confirmation/query/confirmation.query.view.js')
const LoadingView = require('../loading/loading.view.js')
const QueryAnnotationsView = require('../query-annotations/query-annotations.view.js')
const properties = require('../../js/properties.js')
const React = require('react')
import ResultsExport from '../../react-component/container/results-export'

const NOT_CLONEABLE_ATTRIBUTES = ['id', 'result', 'hasBeenSaved']

const createDuplicateQuery = attributes => {
  let clonedAttributes = JSON.parse(JSON.stringify(attributes))
  return _.omit(clonedAttributes, NOT_CLONEABLE_ATTRIBUTES)
}

module.exports = Marionette.ItemView.extend({
  template: template,
  tagName: CustomElements.register('query-interactions'),
  className: 'composed-menu',
  modelEvents: {},
  selectionInterface: store,
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
    'click .interaction-export': 'handleExport',
    click: 'handleClick',
  },
  ui: {},
  initialize: function() {
    if (!this.model.get('result')) {
      this.startListeningToSearch()
    }
    this.handleResult()
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
    this.listenTo(
      this.selectionInterface.getSelectedResults(),
      'update add remove reset',
      this.handleExportSelected
    )
  },
  onRender: function() {
    this.handleLocal()
    this.handleExportSelected()
  },
  handleLocal: function() {
    this.$el.toggleClass('is-local', this.model.isLocal())
  },
  handleExportSelected: function() {
    const disabled = this.selectionInterface.getSelectedResults().length < 1
    this.$el
      .find('.interaction-export')
      .first()
      .toggleClass('is-disabled', disabled)
  },
  startListeningToSearch: function() {
    this.listenToOnce(this.model, 'change:result', this.startListeningForResult)
  },
  startListeningForResult: function() {
    this.listenToOnce(this.model.get('result'), 'sync error', this.handleResult)
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
    const copyAttributes = createDuplicateQuery(this.model.attributes)
    const newQuery = new this.model.constructor(copyAttributes)
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
    lightboxInstance.showContent(
      new QueryFeedbackView({
        model: this.model,
      })
    )
  },
  handleAnnotations: function() {
    lightboxInstance.model.updateTitle('Search Notes')
    lightboxInstance.model.open()
    lightboxInstance.showContent(
      new QueryAnnotationsView({
        model: this.model,
      })
    )
  },
  handleExport: function() {
    const Content = () => {
      const selectedResults = store
        .getSelectedResults()
        .toJSON()
        .map(result => {
          return {
            id: result['metacard']['id'],
            source: result['metacard']['properties']['source-id'],
          }
        })
      return <ResultsExport selectedResults={selectedResults} />
    }

    lightboxInstance.model.updateTitle('Export Results')
    lightboxInstance.model.open()
    lightboxInstance.showContent(<Content />)
  },
  handleResult: function() {
    this.$el.toggleClass('has-results', this.model.get('result') !== undefined)
  },
  handleClick: function() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
})
