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

const Backbone = require('backbone');
const Marionette = require('marionette');
const $ = require('jquery');
const template = require('./metacard-associations.hbs');
const CustomElements = require('../../js/CustomElements.js');
const store = require('../../js/store.js');
const LoadingCompanionView = require('../loading-companion/loading-companion.view.js');
const AssociationsMenuView = require('../associations-menu/associations-menu.view.js');
const AssociationCollectionView = require('../association/association.collection.view.js');
const AssociationCollection = require('../association/association.collection.js');
const AssociationGraphView = require('../associations-graph/associations-graph.view.js');

module.exports = Marionette.LayoutView.extend({
  setDefaultModel: function() {
    this.model = this.selectionInterface.getSelectedResults().first()
  },
  regions: {
    associationsMenu: '> .content-menu',
    associationsList: '> .editor-content',
    associationsGraph: '> .content-graph',
  },
  template: template,
  tagName: CustomElements.register('metacard-associations'),
  selectionInterface: store,
  events: {
    'click > .list-footer .footer-add': 'handleAdd',
    'click > .editor-footer .footer-edit': 'handleEdit',
    'click > .editor-footer .footer-cancel': 'handleCancel',
    'click > .editor-footer .footer-save': 'handleSave',
  },
  _associationCollection: undefined,
  _knownMetacards: undefined,
  initialize: function(options) {
    this.selectionInterface =
      options.selectionInterface || this.selectionInterface
    if (!options.model) {
      this.setDefaultModel()
    }
    this.handleType()
    this.getAssociations()
    this.setupListeners()
  },
  setupListeners: function() {
    this.listenTo(
      this._associationCollection,
      'reset add remove update change',
      this.handleFooter
    )
  },
  getAssociations: function() {
    this.clearAssociations()
    LoadingCompanionView.beginLoading(this)
    $.get('./internal/associations/' + this.model.get('metacard').get('id'))
      .then(
        function(response) {
          if (!this.isDestroyed && this.associationsMenu !== undefined) {
            this._originalAssociations = JSON.parse(JSON.stringify(response))
            this._associations = response
            this.parseAssociations()
            this.onBeforeShow()
          }
        }.bind(this)
      )
      .always(
        function() {
          LoadingCompanionView.endLoading(this)
        }.bind(this)
      )
  },
  clearAssociations: function() {
    if (!this._knownMetacards) {
      this._knownMetacards = new Backbone.Collection()
    }
    if (!this._associationCollection) {
      this._associationCollection = new AssociationCollection()
    }
    this._associationCollection.reset()
  },
  parseAssociations: function() {
    this.clearAssociations()
    this._associations.forEach(
      function(association) {
        this._knownMetacards.add([association.parent, association.child])
        this._associationCollection.add({
          parent: association.parent.id,
          child: association.child.id,
          relationship:
            association.relation === 'metacard.associations.derived'
              ? 'derived'
              : 'related',
        })
      }.bind(this)
    )
  },
  onBeforeShow: function() {
    this.showAssociationsMenuView()
    this.showAssociationsListView()
    this.showGraphView()
    this.handleFooter()
    this.setupMenuListeners()
    this.handleFilter()
    this.handleDisplay()
  },
  showGraphView: function() {
    this.associationsGraph.show(
      new AssociationGraphView({
        collection: this._associationCollection,
        selectionInterface: this.selectionInterface,
        knownMetacards: this._knownMetacards,
        currentMetacard: this.model,
      })
    )
  },
  showAssociationsMenuView: function() {
    this.associationsMenu.show(new AssociationsMenuView())
  },
  showAssociationsListView: function() {
    this.associationsList.show(
      new AssociationCollectionView({
        collection: this._associationCollection,
        selectionInterface: this.selectionInterface,
        knownMetacards: this._knownMetacards,
        currentMetacard: this.model,
      })
    )
    this.associationsList.currentView.turnOffEditing()
  },
  setupMenuListeners: function() {
    this.listenTo(
      this.associationsMenu.currentView.getFilterMenuModel(),
      'change:value',
      this.handleFilter
    )
    this.listenTo(
      this.associationsMenu.currentView.getDisplayMenuModel(),
      'change:value',
      this.handleDisplay
    )
  },
  handleFilter: function() {
    const filter = this.associationsMenu.currentView
      .getFilterMenuModel()
      .get('value')[0];
    this.$el.toggleClass('filter-by-parent', filter === 'parent')
    this.$el.toggleClass('filter-by-child', filter === 'child')
    this.associationsGraph.currentView.handleFilter(filter)
  },
  handleDisplay: function() {
    const filter = this.associationsMenu.currentView
      .getDisplayMenuModel()
      .get('value')[0];
    this.$el.toggleClass('show-list', filter === 'list')
    this.$el.toggleClass('show-graph', filter === 'graph')
    this.associationsGraph.currentView.fitGraph()
  },
  handleEdit: function() {
    this.turnOnEditing()
  },
  handleCancel: function() {
    this._associations = JSON.parse(JSON.stringify(this._originalAssociations))
    this.parseAssociations()
    this.onBeforeShow()
    this.turnOffEditing()
  },
  turnOnEditing: function() {
    this.$el.toggleClass('is-editing', true)
    this.associationsList.currentView.turnOnEditing()
    this.associationsGraph.currentView.turnOnEditing()
  },
  turnOffEditing: function() {
    this.$el.toggleClass('is-editing', false)
    this.associationsList.currentView.turnOffEditing()
    this.associationsGraph.currentView.turnOffEditing()
  },
  handleSave: function() {
    LoadingCompanionView.beginLoading(this)
    const data = this._associationCollection.toJSON();
    data.forEach(function(association) {
      association.parent = {
        id: association.parent,
      }
      association.child = {
        id: association.child,
      }
      association.relation =
        association.relationship === 'related'
          ? 'metacard.associations.related'
          : 'metacard.associations.derived'
    })
    $.ajax({
      url: './internal/associations/' + this.model.get('metacard').get('id'),
      data: JSON.stringify(data),
      method: 'PUT',
      contentType: 'application/json',
    }).always(
      function(response) {
        setTimeout(
          function() {
            if (!this.isDestroyed) {
              this.getAssociations()
              this.turnOffEditing()
            }
          }.bind(this),
          1000
        )
      }.bind(this)
    )
  },
  handleFooter: function() {
    this.$el
      .find('> .list-footer .footer-text')
      .html(this._associationCollection.length + ' association(s)')
  },
  handleAdd: function() {
    this.associationsList.currentView.collection.add({
      parent: this.model.get('metacard').id,
      child: this.model.get('metacard').id,
    })
  },
  handleType: function() {
    this.$el.toggleClass('is-workspace', this.model.isWorkspace())
    this.$el.toggleClass('is-resource', this.model.isResource())
    this.$el.toggleClass('is-revision', this.model.isRevision())
    this.$el.toggleClass('is-deleted', this.model.isDeleted())
    this.$el.toggleClass('is-remote', this.model.isRemote())
  },
})
