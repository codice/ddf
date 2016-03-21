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
/*global define, setInterval, clearInterval*/
define([
    'jquery',
    'underscore',
    'marionette',
    'js/model/Workspace',
    'backbone',
    'direction',
    'wreqr',
    'moment',
    'text!templates/workspace/workspacePanel.handlebars',
    'text!templates/workspace/workspaceList.handlebars',
    'text!templates/workspace/workspaceItem.handlebars',
    'text!templates/workspace/workspaceAdd.handlebars',
    'text!templates/workspace/workspace.handlebars',
    'text!templates/workspace/workspaceQueryItem.handlebars',
    'text!templates/workspace/workspaceMetacardItem.handlebars',
    'text!templates/workspace/workspaceVisibility.handlebars',
    'text!templates/workspace/workspaceContainer.handlebars',
    'js/view/WorkspaceSaveResults.view',
    'maptype',
    'js/view/WorkspaceControl.view',
    'js/view/sliding.region',
    'js/view/Query.view',
    'js/model/Query',
    'js/view/MetacardList.view',
    'js/view/MetacardDetail.view',
    'js/view/Search.view',

    'component/side-panel/index',
    // Load non attached libs and plugins
    'backboneundo',
    'perfectscrollbar'
], function ($, _, Marionette, Workspace, Backbone, dir, wreqr, moment, workspacePanel,
             workspaceList, workspaceItem, workspaceAdd, workspace, workspaceQueryItem,
             workspaceMetacardItem, workspaceVisibility, workspaceContainer, WorkspaceSaveResults,
             maptype, WorkspaceControl, SlidingRegion, QueryView, QueryModel) {
    'use strict';
    var WorkspaceView = {};

    WorkspaceView.WorkspaceAdd = Marionette.ItemView.extend({
        template: workspaceAdd,
        events: {
            'click .submit': 'addWorkspace',
            'click #cancel': 'cancel',
            'keypress #workspaceName': 'addWorkspaceOnEnter'
        },
        initialize: function(options) {
            this.collection = options.collection;
        },
        addWorkspaceOnEnter: function(e) {
            if (e.keyCode === 13) {
                this.addWorkspace();
                e.preventDefault();
            }
        },
        addWorkspace: function() {
            var workspace = new Workspace.Model({name: this.$('#workspaceName').val()});
            this.collection.add(workspace);

            this.collection.parents[0].save();
            wreqr.vent.trigger('workspace:list', this.model);
        },
        cancel: function() {
            this.destroy();
            wreqr.vent.trigger('workspace:list', this.model);
        }
    });

    WorkspaceView.MetacardItem = Marionette.ItemView.extend({
        template: workspaceMetacardItem,
        tagName: 'tr',
        className: 'workspace-row',
        events: {
            'click .workspace-remove': 'removeMetacard',
            'click': 'showMetacard'
        },
        modelEvents: { 'change': 'render' },
        removeMetacard: function () {
        },
        showMetacard: function () {
            wreqr.vent.trigger('workspace:metacard', this.model);
        }
    });

    WorkspaceView.SearchItem = Marionette.ItemView.extend({
        template: workspaceQueryItem,
        tagName: 'tr',
        className: 'workspace-row',
        events: {
            'click .workspace-remove': 'removeSearch',
            'click .workspace-edit': 'editSearch',
            'click': 'showSearch'
        },
        modelEvents: {
            'change': 'render',
            'change:result': 'render'
        },
        initialize: function () {
            if (this.model.get('result')) {
                this.listenTo(this.model.get('result'), 'change', this.render);
            }
            this.updateInterval = setInterval(_.bind(this.update, this), 60000);
        },
        serializeData: function () {
            var working = false, result = this.model.get('result'), hits, initiated;
            if (result) {
                var statuses = result.get('status');
                if (result.get('hits')) {
                    hits = result.get('hits');
                }
                if (result.get('initiated')) {
                    initiated = moment(result.get('initiated')).fromNow();
                }
                if (statuses) {
                    statuses.forEach(function (status) {
                        if (status.get('state') === 'ACTIVE') {
                            working = true;
                        }
                    });
                }
            }
            return _.extend(this.model.toJSON(), {
                working: working,
                initiated: initiated,
                hits: hits
            });
        },
        update: function () {
            if (!this.model.get('editing')) {
                this.render();
            }
        },
        editSearch: function () {
            wreqr.vent.trigger('workspace:editcancel');
            wreqr.vent.trigger('workspace:searchedit', dir.forward, this.model, this.model.collection.indexOf(this.model));
        },
        removeSearch: function () {
            this.model.collection.remove(this.model);
        },
        showSearch: function (event) {
            if ([
                    'edit',
                    'remove'
                ].indexOf(event.target.id) === -1) {
                wreqr.vent.trigger('workspace:editcancel');
                var progressFunction = function (value, model) {
                    model.mergeLatest();
                    wreqr.vent.trigger('map:clear');
                    wreqr.vent.trigger('map:results', model, false);
                };
                this.model.startSearch(progressFunction);
                if (this.model.get('result') && this.model.get('result').get('status')) {
                    this.model.get('result').get('status').forEach(function (model) {
                        model.set('state', 'ACTIVE');
                    });
                }
                wreqr.vent.trigger('workspace:results', dir.forward, this.model);
            }
        },
        onDestroy: function () {
            clearInterval(this.updateInterval);
        }
    });

    WorkspaceView.MetacardList = Marionette.CollectionView.extend({
        childView: WorkspaceView.MetacardItem,
        tagName: 'table',
        className: 'table'
    });

    WorkspaceView.SearchList = Marionette.CollectionView.extend({
        childView: WorkspaceView.SearchItem,
        tagName: 'table',
        className: 'table'
    });

    WorkspaceView.Workspace = Marionette.LayoutView.extend({
        template: workspace,
        className: 'search-form',
        initialize: function () {
            this.listenTo(wreqr.vent, 'workspace:edit', this.editMode);
            this.listenTo(wreqr.vent, 'workspace:editcancel', this.cancelMode);
            this.listenTo(wreqr.vent, 'workspace:searcheditcancel', this.cancelMode);
            this.listenTo(wreqr.vent, 'workspace:save', this.doneMode);
            this.undoManager = new Backbone.UndoManager();
            this.undoManager.register(this.model.get('searches'));
        },
        regions: { workspaceSearchPanelRegion: '#workspaceSearchPanel' },
        events: {
            'click #addSearch': 'addSearch',
            'click #view-records': 'viewSavedRecords'
        },
        addSearch: function () {
            var model = new QueryModel.Model();
            this.model.get('searches').add(model);
            wreqr.vent.trigger('workspace:searchedit', dir.forward, model);
        },
        viewSavedRecords: function () {
            wreqr.vent.trigger('workspace:results', dir.forward, this.model.get('metacards'));
        },
        onRender: function () {
            var searches = this.model.get('searches');
            var editing = this.model.get('editing');
            if (searches && searches.length) {
                _.each(searches.models, function (search) {
                    search.set('editing', editing);
                });
            }
            this.workspaceSearchPanelRegion.show(new WorkspaceView.SearchList({ collection: this.model.get('searches') }));
        },
        editMode: function () {
            this.model.set('editing', true, { silent: true });
            this.undoManager.startTracking();
            this.render();
        },
        doneMode: function () {
            this.undoManager.stopTracking();
            this.undoManager.clear();
            this.model.unset('editing', { silent: true });
            this.render();
        },
        cancelMode: function () {
            this.undoManager.undoAll();
            this.undoManager.stopTracking();
            this.undoManager.clear();
            this.model.unset('editing', { silent: true });
            this.render();
        }
    });

    WorkspaceView.WorkspaceItem = Marionette.ItemView.extend({
        template: workspaceItem,
        tagName: 'tr',
        className: 'workspace-row',
        events: {
            'click .workspace-remove': 'removeWorkspace',
            'click': 'showWorkspace'
        },
        modelEvents: { 'change': 'render' },
        editing: false,
        initialize: function () {
            var view = this;
            this.listenTo(wreqr.vent, 'workspace:editall', this.editMode);
            this.listenTo(wreqr.vent, 'workspace:saveall', this.doneMode);
            this.listenTo(wreqr.vent, 'workspace:editcancel', this.cancelMode);
            this.undoManager = new Backbone.UndoManager();
            this.undoManager.register(this.model, this.model.collection);
            if (this.model.get('searches')) {
                var searches = this.model.get('searches');
                searches.forEach(function (search) {
                    var result = search.get('result');
                    if (result) {
                        view.listenTo(result, 'change', view.render);
                    }
                });
            }
            if (this.model.get('metacards')) {
                this.listenTo(this.model.get('metacards'), 'change', this.render);
            }
            _.bindAll(this);
            this.modelBinder = new Backbone.ModelBinder();
        },
        serializeData: function () {
            var working = false, hits, searches = this.model.get('searches');
            if (searches) {
                searches.forEach(function (search) {
                    var result = search.get('result');
                    if (result) {
                        if (result.get('hits')) {
                            if (!hits)
                                hits = 0;
                            hits += result.get('hits');
                        }
                        var statuses = result.get('status');
                        if (statuses) {
                            statuses.forEach(function (status) {
                                if (status.get('state') === 'ACTIVE') {
                                    working = true;
                                }
                            });
                        }
                    }
                });
            }
            return _.extend(this.model.toJSON(), {
                working: working,
                hits: hits,
                editing: this.editing
            });
        },
        editMode: function () {
            this.undoManager.startTracking();
            this.editing = true;
            this.render();
        },
        doneMode: function () {
            this.undoManager.stopTracking();
            this.undoManager.clear();
            this.editing = false;
            this.render();
        },
        cancelMode: function () {
            this.undoManager.undoAll();
            this.undoManager.stopTracking();
            this.undoManager.clear();
            this.editing = false;
            this.render();
        },
        showWorkspace: function () {
            if (!this.editing) {
                wreqr.vent.trigger('workspace:show', dir.forward, this.model);
            }
        },
        removeWorkspace: function () {
            this.model.collection.remove(this.model);
        },
        onRender: function () {
            if (this.editing) {
                var bindings = { name: '#name' };
                this.modelBinder.bind(this.model, this.el, bindings);
            }
        }
    });

    WorkspaceView.WorkspaceList = Marionette.CollectionView.extend({
        template: workspaceList,
        childView: WorkspaceView.WorkspaceItem,
        tagName: 'table',
        className: 'table workspace-table'
    });

    WorkspaceView.WorkspacesLayoutView = Marionette.LayoutView.extend({
        template: workspaceList,
        className: 'height-full',
        regions: {
            workspaceControlRegion: '#workspaceControl',
            workspaceRegion: {
                selector: '#workspace',
                regionClass: SlidingRegion
            }
        },
        modelEvents: { 'change': 'render' },
        initialize: function () {
            _.bindAll(this);
            this.listenTo(wreqr.vent, 'workspace:tabshown', this.setupEvents);
            this.listenTo(wreqr.vent, 'workspace:save', this.workspaceSave);
            this.listenTo(wreqr.vent, 'workspace:editall', this.editWorkspaceList);
            this.listenTo(wreqr.vent, 'workspace:saveall', this.workspaceSave);
            this.listenTo(wreqr.vent, 'workspace:searcheditcancel', this.workspaceCancelEdit);
            this.undoManager = new Backbone.UndoManager();
            this.undoManager.register(this.model.get('workspaces'));
        },
        setupEvents: function (tabHash) {
            if (this.currentSearch && tabHash === '#workspaces') {
                wreqr.vent.trigger('map:clear');
                wreqr.vent.trigger('map:results', this.currentSearch.get('result'), false);
                if (this.currentSearch) {
                    this.updateMapPrimitive();
                }
            }
            _.each(this.model.get('workspaces').models, function (workspace) {
                workspace.unset('editing', { silent: true });
            });
            if (this.model.get('editingList')) {
                this.cancelEditWorkspaceList();
            }
            if (tabHash === '#workspaces') {
                this.listenTo(wreqr.vent, 'workspace:show', this.showWorkspace);
                this.listenTo(wreqr.vent, 'workspace:results', this.showWorkspaceResults);
                this.listenTo(wreqr.vent, 'metacard:selected', this.showWorkspaceMetacard);
                this.listenTo(wreqr.vent, 'workspace:new', this.showWorkspaceAdd);
                this.listenTo(wreqr.vent, 'workspace:searchedit', this.showWorkspaceSearchEdit);
                this.listenTo(wreqr.vent, 'workspace:editcancel', this.cancelEditWorkspaceList);
                this.listenTo(wreqr.vent, 'workspace:list', this.showWorkspaceList);
                this.listenTo(wreqr.vent, 'workspace:cancel', this.workspaceCancelAdd);
                this.listenTo(wreqr.vent, 'workspace:saveresults', this.saveResultsToWorkspace);
                this.listenTo(wreqr.vent, 'workspace:resultssavecancel', this.cancelResultsToWorkspace);
                this.workspaceRegion.show(undefined, dir.none);
            } else {
                this.stopListening(wreqr.vent, 'workspace:show');
                this.stopListening(wreqr.vent, 'workspace:results');
                this.stopListening(wreqr.vent, 'metacard:selected');
                this.stopListening(wreqr.vent, 'workspace:new');
                this.stopListening(wreqr.vent, 'workspace:searchedit');
                this.stopListening(wreqr.vent, 'workspace:editcancel');
                this.stopListening(wreqr.vent, 'workspace:list');
                this.stopListening(wreqr.vent, 'workspace:cancel');
                this.stopListening(wreqr.vent, 'workspace:saveresults');
                this.stopListening(wreqr.vent, 'workspace:resultssavecancel');
                this.workspaceRegion.destroy();
            }
        },
        saveResultsToWorkspace: function (search, records) {
            this.workspaceRegion.show(new WorkspaceSaveResults({
                model: this.model,
                search: search,
                records: records
            }), dir.forward);
        },
        cancelResultsToWorkspace: function () {
            this.showWorkspaceResults(dir.backward);
        },
        workspaceCancelAdd: function (model) {
            if (model && !model.get('name')) {
                if (this.currentWorkspace) {
                    this.currentWorkspace.get('searches').remove(model);
                }
            }
            wreqr.vent.trigger('workspace:show', dir.backward, this.currentWorkspace);
        },
        workspaceSave: function (search) {
            var view = this;
            if (view.currentWorkspace) {
                if (search) {
                    if (_.isUndefined(this.currentSearchIndex) || _.isNull(this.currentSearchIndex) || _.isNaN(this.currentSearchIndex) || this.currentSearchIndex < 0) {
                        view.currentWorkspace.get('searches').add(search);
                    } else {
                        view.currentWorkspace.get('searches').models[this.currentSearchIndex] = search;
                    }
                }
            }
        }
    });

    return WorkspaceView;
});
