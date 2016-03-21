/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
define([
    'jquery',
    'underscore',
    'marionette',
    'backbone',
    'wreqr',
    'text!templates/search/searchControl.handlebars',
    'direction'
], function ($, _, Marionette, Backbone, wreqr, workspaceControlTemplate, dir) {
    'use strict';
    var WorkspaceControl = {};
    WorkspaceControl.WorkspaceControlModel = Backbone.Model.extend({
        defaults: {
            back: 'Add',
            forward: 'Edit',
            title: 'Workspaces',
            currentState: 'list',
            showChevronBack: false,
            showChevronForward: false
        },
        setInitialState: function () {
            this.setWorkspaceListState();
        },
        setWorkspaceListState: function () {
            this.set(this.defaults);
        },
        setWorkspaceListEditState: function () {
            this.set({
                back: 'Cancel',
                forward: 'Done',
                title: 'Workspaces',
                currentState: 'list',
                showChevronBack: false,
                showChevronForward: false
            });
        },
        setWorkspaceViewState: function () {
            var model = this.model;
            this.set({
                back: 'Workspaces',
                title: model ? model.get('name') : 'View Workspace',
                forward: 'Edit',
                currentState: 'view',
                showChevronBack: true,
                showChevronForward: false
            });
        },
        setWorkspaceEditState: function () {
            var model = this.model;
            this.set({
                back: 'Cancel',
                title: model ? model.get('name') : 'Edit Workspace',
                forward: 'Done',
                currentState: 'view',
                showChevronBack: false,
                showChevronForward: false
            });
        },
        setWorkspaceNewState: function () {
            this.set({
                back: 'Workspaces',
                title: 'Add Workspace',
                forward: '',
                currentState: 'add',
                showChevronBack: false,
                showChevronForward: false
            });
        },
        setWorkspaceQueryEditState: function () {
            this.set({
                back: 'Workspace',
                title: 'Add/Edit Search',
                forward: '',
                currentState: 'edit',
                showChevronBack: true,
                showChevronForward: false
            });
        },
        setWorkspaceResultsState: function (direction, model) {
            this.set({
                back: 'Workspace',
                title: 'Results',
                forward: model && model.models ? '' : 'Save',
                currentState: 'results',
                showChevronBack: true,
                showChevronForward: false
            });
        },
        setWorkspaceResultsSelectState: function () {
            this.set({
                back: 'Workspace',
                title: 'Results',
                forward: 'Done',
                currentState: 'results',
                showChevronBack: true,
                showChevronForward: false
            });
        },
        setWorkspaceMetacardState: function () {
            this.set({
                back: 'Results',
                title: 'Record',
                forward: '',
                currentState: 'metacard',
                showChevronBack: true,
                showChevronForward: false
            });
        },
        setSelectWorkspaceState: function () {
            this.set({
                back: '',
                title: 'Select Workspace',
                forward: '',
                currentState: 'select'
            });
        },
        revertToPrevious: function () {
            this.set(this._previousAttributes);
        }
    });
    WorkspaceControl.WorkspaceControlView = Marionette.ItemView.extend({
        template: workspaceControlTemplate,
        events: {
            'click .back': 'action',
            'click .forward': 'action'
        },
        modelEvents: { 'change': 'render' },
        initialize: function () {
            this.model = new WorkspaceControl.WorkspaceControlModel({ model: this.options.workspace });
            this.setupEvents('#workspaces');
            this.listenTo(wreqr.vent, 'workspace:tabshown', this.setupEvents);
        },
        setupEvents: function (tabHash) {
            if (tabHash === '#workspaces') {
                this.listenTo(wreqr.vent, 'workspace:show', _.bind(this.model.setWorkspaceViewState, this.model));
                this.listenTo(wreqr.vent, 'workspace:results', _.bind(this.model.setWorkspaceResultsState, this.model));
                this.listenTo(wreqr.vent, 'metacard:selected', _.bind(this.model.setWorkspaceMetacardState, this.model));
                this.listenTo(wreqr.vent, 'workspace:new', _.bind(this.model.setWorkspaceNewState, this.model));
                this.listenTo(wreqr.vent, 'workspace:searchedit', _.bind(this.model.setWorkspaceQueryEditState, this.model));
                this.listenTo(wreqr.vent, 'workspace:edit', _.bind(this.model.setWorkspaceEditState, this.model));
                this.listenTo(wreqr.vent, 'workspace:list', _.bind(this.model.setWorkspaceListState, this.model));
                this.listenTo(wreqr.vent, 'workspace:editall', _.bind(this.model.setWorkspaceListEditState, this.model));
                this.listenTo(wreqr.vent, 'workspace:save', _.bind(this.model.setWorkspaceViewState, this.model));
                this.listenTo(wreqr.vent, 'workspace:searcheditcancel', _.bind(this.model.setWorkspaceViewState, this.model));
                this.listenTo(wreqr.vent, 'workspace:saveresults', _.bind(this.model.setSelectWorkspaceState, this.model));
                this.listenTo(wreqr.vent, 'workspace:resultssavecancel', _.bind(this.model.revertToPrevious, this.model));
                this.listenTo(wreqr.vent, 'workspace:searchsavecancel', _.bind(this.model.revertToPrevious, this.model));
                this.listenTo(wreqr.vent, 'search:resultsselect', _.bind(this.model.setWorkspaceResultsSelectState, this.model));
                this.resetViewState();
            } else {
                this.stopListening(wreqr.vent, 'workspace:show');
                this.stopListening(wreqr.vent, 'workspace:results');
                this.stopListening(wreqr.vent, 'metacard:selected');
                this.stopListening(wreqr.vent, 'workspace:new');
                this.stopListening(wreqr.vent, 'workspace:searchedit');
                this.stopListening(wreqr.vent, 'workspace:edit');
                this.stopListening(wreqr.vent, 'workspace:list');
                this.stopListening(wreqr.vent, 'workspace:editall');
                this.stopListening(wreqr.vent, 'workspace:save');
                this.stopListening(wreqr.vent, 'workspace:searcheditcancel');
                this.stopListening(wreqr.vent, 'workspace:saveresults');
                this.stopListening(wreqr.vent, 'workspace:resultssavecancel');
                this.stopListening(wreqr.vent, 'workspace:searchsavecancel');
                this.stopListening(wreqr.vent, 'search:resultsselect');
            }
        },
        resetViewState: function () {
            var state = this.model.get('currentState');
            switch (state) {
            case 'list':
                this.model.setWorkspaceListState();
                break;
            case 'view':
                this.model.setWorkspaceViewState();
                break;
            case 'results':
                this.model.setWorkspaceResultsState();
                break;
            }
        },
        action: function (e) {
            var state = this.model.get('currentState');
            var id = e.target.id;
            switch (state) {
            case 'list':
                if (id === 'Add') {
                    wreqr.vent.trigger('workspace:new', dir.forward);
                }
                if (id === 'Edit') {
                    wreqr.vent.trigger('workspace:editall');
                }
                if (id === 'Cancel') {
                    this.model.setWorkspaceListState();
                    wreqr.vent.trigger('workspace:editcancel');
                }
                if (id === 'Done') {
                    this.model.setWorkspaceListState();
                    wreqr.vent.trigger('workspace:saveall');
                }
                break;
            case 'add':
                if (id === 'Workspaces') {
                    wreqr.vent.trigger('workspace:list', dir.backward);
                }
                break;
            case 'edit':
                if (id === 'Workspace') {
                    wreqr.vent.trigger('workspace:show', dir.backward);
                }
                break;
            case 'view':
                if (id === 'Workspaces') {
                    wreqr.vent.trigger('workspace:list', dir.backward);
                }
                if (id === 'Edit') {
                    wreqr.vent.trigger('workspace:edit');
                }
                if (id === 'Cancel') {
                    wreqr.vent.trigger('workspace:searcheditcancel');
                }
                if (id === 'Done') {
                    wreqr.vent.trigger('workspace:save');
                }
                break;
            case 'results':
                if (id === 'Workspace') {
                    wreqr.vent.trigger('search:clearfilters');
                    wreqr.vent.trigger('workspace:show', dir.backward);
                }
                if (id === 'Save') {
                    wreqr.vent.trigger('search:resultsselect');
                }
                if (id === 'Done') {
                    this.model.setWorkspaceResultsState();
                    wreqr.vent.trigger('search:resultssave');
                }
                break;
            case 'metacard':
                if (id === 'Results') {
                    wreqr.vent.trigger('workspace:results', dir.backward);
                }
                break;
            }
        }
    });
    return WorkspaceControl;
});
