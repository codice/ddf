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
        'icanhaz',
        'wreqr',
        'text!templates/search/searchControl.handlebars',
        'direction'
    ],
    function ($, _, Marionette, Backbone, ich, wreqr, workspaceControlTemplate, dir) {
        "use strict";
        var WorkspaceControl = {};

        ich.addTemplate('workspaceControlTemplate', workspaceControlTemplate);

        WorkspaceControl.WorkspaceControlModel = Backbone.Model.extend({
            defaults: {
                back: 'Add',
                forward: 'Edit',
                title: 'Workspaces',
                currentState: 'list',
                showChevronBack: false,
                showChevronForward: false
            },
            setInitialState: function() {
                this.setWorkspaceListState();
            },
            setWorkspaceListState: function() {
                this.set(this.defaults);
            },
            setWorkspaceListEditState: function() {
                this.set({
                    back: 'Add',
                    forward: 'Done',
                    title: 'Workspaces',
                    currentState: 'list',
                    showChevronBack: false,
                    showChevronForward: false
                });
            },
            setWorkspaceViewState: function() {
                var model;
                if (wreqr.reqres.hasHandler('workspace:getCurrent')) {
                    model = wreqr.reqres.request('workspace:getCurrent');
                }
                this.set({
                    back: 'Workspaces',
                    title: model ? model.get('name') : 'View Workspace',
                    forward: 'Edit',
                    currentState: 'view',
                    showChevronBack: true,
                    showChevronForward: false
                });
            },
            setWorkspaceEditState: function() {
                var model;
                if (wreqr.reqres.hasHandler('workspace:getCurrent')) {
                    model = wreqr.reqres.request('workspace:getCurrent');
                }
                this.set({
                    back: 'Workspaces',
                    title: model ? model.get('name') : 'Edit Workspace',
                    forward: 'Done',
                    currentState: 'view',
                    showChevronBack: true,
                    showChevronForward: false
                });
            },
            setWorkspaceNewState: function() {
                this.set({
                    back: 'Workspaces',
                    title: 'Add Workspace',
                    forward: '',
                    currentState: 'add',
                    showChevronBack: true,
                    showChevronForward: false
                });
            },
            setWorkspaceQueryEditState: function() {
                this.set({
                    back: 'Workspace',
                    title: 'Add/Edit Search',
                    forward: '',
                    currentState: 'edit',
                    showChevronBack: true,
                    showChevronForward: false
                });
            },
            setWorkspaceResultsState: function() {
                this.set({
                    back: 'Workspace',
                    title: 'Results',
                    forward: '',
                    currentState: 'results'
                });
            },
            setWorkspaceMetacardState: function() {
                this.set({
                    back: 'Results',
                    title: 'Record',
                    forward: '',
                    currentState: 'metacard'
                });
            }
        });

        WorkspaceControl.WorkspaceControlView = Marionette.ItemView.extend({
            template : 'workspaceControlTemplate',
            model: new WorkspaceControl.WorkspaceControlModel(),
            events: {
                'click .back': 'action',
                'click .forward': 'action'
            },

            modelEvents: {
                'change': 'render'
            },

            initialize: function() {
                this.setupEvents();
                wreqr.vent.on('workspace:tabshown', _.bind(this.setupEvents, this));
            },

            setupEvents: function(tabHash) {
                if(tabHash === '#workspaces') {
                    wreqr.vent.on('workspace:show', _.bind(this.model.setWorkspaceViewState, this.model));
                    wreqr.vent.on('workspace:results', _.bind(this.model.setWorkspaceResultsState, this.model));
                    wreqr.vent.on('metacard:selected', _.bind(this.model.setWorkspaceMetacardState, this.model));
                    wreqr.vent.on('workspace:new', _.bind(this.model.setWorkspaceNewState, this.model));
                    wreqr.vent.on('workspace:searchedit', _.bind(this.model.setWorkspaceQueryEditState, this.model));
                    wreqr.vent.on('workspace:edit', _.bind(this.model.setWorkspaceEditState, this.model));
                    wreqr.vent.on('workspace:list', _.bind(this.model.setWorkspaceListState, this.model));
                    wreqr.vent.on('workspace:saveall', _.bind(this.model.setWorkspaceListState, this.model));
                    wreqr.vent.on('workspace:editall', _.bind(this.model.setWorkspaceListEditState, this.model));
                    wreqr.vent.on('workspace:save', _.bind(this.model.setWorkspaceViewState, this.model));
                } else {
                    wreqr.vent.off('workspace:show', _.bind(this.model.setWorkspaceViewState, this.model));
                    wreqr.vent.off('workspace:results', _.bind(this.model.setWorkspaceResultsState, this.model));
                    wreqr.vent.off('metacard:selected', _.bind(this.model.setWorkspaceMetacardState, this.model));
                    wreqr.vent.off('workspace:new', _.bind(this.model.setWorkspaceNewState, this.model));
                    wreqr.vent.off('workspace:searchedit', _.bind(this.model.setWorkspaceQueryEditState, this.model));
                    wreqr.vent.off('workspace:edit', _.bind(this.model.setWorkspaceEditState, this.model));
                    wreqr.vent.off('workspace:list', _.bind(this.model.setWorkspaceListState, this.model));
                    wreqr.vent.off('workspace:saveall', _.bind(this.model.setWorkspaceListState, this.model));
                    wreqr.vent.off('workspace:editall', _.bind(this.model.setWorkspaceListEditState, this.model));
                    wreqr.vent.off('workspace:save', _.bind(this.model.setWorkspaceViewState, this.model));
                }
            },

            action: function (e) {
                var state = this.model.get('currentState');
                var id = e.target.id;
                switch(state) {
                    case 'list':
                        if(id === 'Add') {
                            wreqr.vent.trigger('workspace:new', dir.forward);
                        }
                        if(id === 'Edit') {
                            wreqr.vent.trigger('workspace:editall');
                        }
                        if(id === 'Done') {
                            wreqr.vent.trigger('workspace:saveall');
                        }
                        break;
                    case 'add':
                        if(id === 'Workspaces') {
                            wreqr.vent.trigger('workspace:list', dir.backward);
                        }
                        break;
                    case 'edit':
                        break;
                    case 'view':
                        if(id === 'Workspaces') {
                            wreqr.vent.trigger('workspace:list', dir.backward);
                        }
                        if(id === 'Edit') {
                            wreqr.vent.trigger('workspace:edit');
                        }
                        if(id === 'Done') {
                            wreqr.vent.trigger('workspace:save');
                        }
                        break;
                    case 'results':
                        if(id === 'Workspace') {
                            wreqr.vent.trigger('workspace:show', dir.backward);
                        }
                        break;
                    case 'metacard':
                        if(id === 'Results') {
                            wreqr.vent.trigger('workspace:results', dir.backward);
                        }
                        break;
                }
            }
        });

        return WorkspaceControl;
    });
