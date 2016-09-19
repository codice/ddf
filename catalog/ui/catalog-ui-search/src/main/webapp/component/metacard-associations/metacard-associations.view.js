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
var Backbone = require('backbone');
var Marionette = require('marionette');
var _ = require('underscore');
var $ = require('jquery');
var template = require('./metacard-associations.hbs');
var CustomElements = require('js/CustomElements');
var store = require('js/store');
var LoadingCompanionView = require('component/loading-companion/loading-companion.view');
var AssociationsMenuView = require('component/associations-menu/associations-menu.view');
var AssociationCollectionView = require('component/association/association.collection.view');
var AssociationCollection = require('component/association/association.collection');
var AssociationGraphView = require('component/associations-graph/associations-graph.view');

module.exports = Marionette.LayoutView.extend({
    setDefaultModel: function() {
        this.model = this.selectionInterface.getSelectedResults().first();
    },
    regions: {
        associationsMenu: '> .content-menu',
        associationsList: '> .editor-content',
        associationsGraph: '> .content-graph'
    },
    template: template,
    tagName: CustomElements.register('metacard-associations'),
    selectionInterface: store,
    events: {
        'click > .list-footer .footer-add': 'handleAdd',
        'click > .editor-footer .footer-edit': 'handleEdit',
        'click > .editor-footer .footer-cancel': 'handleCancel',
        'click > .editor-footer .footer-save': 'handleSave'
    },
    _associationCollection: undefined,
    _knownMetacards: undefined,
    initialize: function(options) {
        this.selectionInterface = options.selectionInterface || this.selectionInterface;
        if (!options.model) {
            this.setDefaultModel();
        }
        this.getAssociations();
        this.setupListeners();
    },
    setupListeners: function() {
        this.listenTo(this._associationCollection, 'reset add remove update change', this.handleFooter);
    },
    getAssociations: function() {
        this.clearAssociations();
        LoadingCompanionView.beginLoading(this);
        $.get('/search/catalog/internal/associations/' + this.model.get('metacard').get('id')).then(function(response) {
            this._originalAssociations = JSON.parse(JSON.stringify(response));
            this._associations = response;
            this.parseAssociations();
            this.onBeforeShow();
        }.bind(this)).always(function() {
            LoadingCompanionView.endLoading(this);
        }.bind(this));
    },
    clearAssociations: function() {
        if (!this._knownMetacards) {
            this._knownMetacards = new Backbone.Collection();
        }
        if (!this._associationCollection) {
            this._associationCollection = new AssociationCollection();
        }
        this._associationCollection.reset();
    },
    parseAssociations: function() {
        this.clearAssociations();
        this._associations.forEach(function(association) {
            this._knownMetacards.add([association.parent, association.child]);
            this._associationCollection.add({
                parent: association.parent.id,
                child: association.child.id,
                relationship: association.relation === 'metacard.associations.derived' ? 'derived' : 'related'
            })
        }.bind(this));
    },
    onBeforeShow: function() {
        this.showAssociationsMenuView();
        this.showAssociationsListView();
        this.showGraphView();
        this.handleFooter();
        this.setupMenuListeners();
        this.handleFilter();
        this.handleDisplay();
    },
    showGraphView: function() {
        this.associationsGraph.show(new AssociationGraphView({
            collection: this._associationCollection,
            selectionInterface: this.selectionInterface,
            knownMetacards: this._knownMetacards,
            currentMetacard: this.model
        }));
    },
    showAssociationsMenuView: function() {
        this.associationsMenu.show(new AssociationsMenuView());
    },
    showAssociationsListView: function() {
        this.associationsList.show(new AssociationCollectionView({
            collection: this._associationCollection,
            selectionInterface: this.selectionInterface,
            knownMetacards: this._knownMetacards,
            currentMetacard: this.model
        }));
        this.associationsList.currentView.turnOffEditing();
    },
    setupMenuListeners: function() {
        this.listenTo(this.associationsMenu.currentView.getFilterMenuModel(), 'change:value', this.handleFilter);
        this.listenTo(this.associationsMenu.currentView.getDisplayMenuModel(), 'change:value', this.handleDisplay);
    },
    handleFilter: function() {
        var filter = this.associationsMenu.currentView.getFilterMenuModel().get('value')[0];
        this.$el.toggleClass('filter-by-parent', filter === 'parent');
        this.$el.toggleClass('filter-by-child', filter === 'child');
        this.associationsGraph.currentView.handleFilter(filter);
    },
    handleDisplay: function() {
        var filter = this.associationsMenu.currentView.getDisplayMenuModel().get('value')[0];
        this.$el.toggleClass('show-list', filter === 'list');
        this.$el.toggleClass('show-graph', filter === 'graph');
        this.associationsGraph.currentView.fitGraph();
    },
    handleEdit: function() {
        this.turnOnEditing();
    },
    handleCancel: function() {
        this._associations = JSON.parse(JSON.stringify(this._originalAssociations));
        this.parseAssociations();
        this.onBeforeShow();
        this.turnOffEditing();
    },
    turnOnEditing: function() {
        this.$el.toggleClass('is-editing', true);
        this.associationsList.currentView.turnOnEditing();
        this.associationsGraph.currentView.turnOnEditing();
    },
    turnOffEditing: function() {
        this.$el.toggleClass('is-editing', false);
        this.associationsList.currentView.turnOffEditing();
        this.associationsGraph.currentView.turnOffEditing();
    },
    handleSave: function() {
        LoadingCompanionView.beginLoading(this);
        var data = this._associationCollection.toJSON();
        data.forEach(function(association) {
            association.parent = {
                id: association.parent
            };
            association.child = {
                id: association.child
            }
            association.relation = association.relationship === 'related' ? 'metacard.associations.related' : 'metacard.associations.derived';
        });
        $.ajax({
            url: '/search/catalog/internal/associations/' + this.model.get('metacard').get('id'),
            data: JSON.stringify(data),
            method: 'PUT',
            contentType: 'application/json'
        }).always(function(response) {
            setTimeout(function() {
                this.getAssociations();
                this.turnOffEditing();
            }.bind(this), 1000);
        }.bind(this));
    },
    handleFooter: function() {
        this.$el.find('> .list-footer .footer-text')
            .html(this._associationCollection.length + ' association(s)');
    },
    handleAdd: function() {
        this.associationsList.currentView.collection.add({
            parent: this.model.get('metacard').id,
            child: this.model.get('metacard').id
        });
    }
});