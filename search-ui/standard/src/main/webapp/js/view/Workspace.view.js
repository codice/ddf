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
        'js/model/Workspace',
        'backbone',
        'direction',
        'icanhaz',
        'wreqr',
        'text!templates/workspace/workspacePanel.handlebars',
        'text!templates/workspace/workspaceList.handlebars',
        'text!templates/workspace/workspaceItem.handlebars',
        'text!templates/workspace/workspaceAdd.handlebars',
        'text!templates/workspace/workspace.handlebars',
        'text!templates/workspace/workspaceQueryItem.handlebars',
        'text!templates/workspace/workspaceMetacardItem.handlebars',
        'maptype',
        'js/view/WorkspaceControl.view',
        'js/view/sliding.region',
        'js/view/Query.view',
        'js/model/Query',
        'js/view/MetacardList.view',
        'js/view/MetacardDetail.view',
        'js/view/Search.view',
        // Load non attached libs and plugins
        'perfectscrollbar'
    ],
    function ($, _, Marionette, Workspace, Backbone, dir, ich, wreqr, workspacePanel, workspaceList,
              workspaceItem, workspaceAdd, workspace, workspaceQueryItem, workspaceMetacardItem,
              maptype, WorkspaceControl, SlidingRegion, QueryView, QueryModel, MetacardList, MetacardDetail,
              Search) {
        "use strict";
        var WorkspaceView = {};

        ich.addTemplate('workspacePanel', workspacePanel);
        ich.addTemplate('workspaceList', workspaceList);
        ich.addTemplate('workspaceItem', workspaceItem);
        ich.addTemplate('workspaceAdd', workspaceAdd);
        ich.addTemplate('workspace', workspace);
        ich.addTemplate('workspaceQueryItem', workspaceQueryItem);
        ich.addTemplate('workspaceMetacardItem', workspaceMetacardItem);

        WorkspaceView.WorkspaceAdd = Marionette.ItemView.extend({
            template: 'workspaceAdd',
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
                }
            },
            addWorkspace: function() {
                var workspace = new Workspace.Model({name: this.$('#workspaceName').val()});
                this.collection.add(workspace);

                this.collection.parents[0].save();
                wreqr.vent.trigger('workspace:list', this.model);
            },
            cancel: function() {
                this.close();
                wreqr.vent.trigger('workspace:list', this.model);
            }
        });

        WorkspaceView.MetacardItem = Marionette.ItemView.extend({
            template: 'workspaceMetacardItem',
            tagName: 'tr',
            className: 'workspace-row',
            events: {
                'click .workspace-remove': 'removeMetacard',
                'click': 'showMetacard'
            },
            modelEvents: {
                'change': 'render'
            },
            removeMetacard: function() {

            },
            showMetacard: function() {
                wreqr.vent.trigger('workspace:metacard', this.model);
            }
        });

        WorkspaceView.SearchItem = Marionette.ItemView.extend({
            template: 'workspaceQueryItem',
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
            editing: false,
            initialize: function() {
                wreqr.vent.on('workspace:edit', _.bind(this.editMode, this));
                wreqr.vent.on('workspace:save', _.bind(this.doneMode, this));
                if(this.model.get('result')) {
                    this.listenTo(this.model.get('result'), 'change', this.render);
                }
            },
            serializeData: function() {
                var working = false, result = this.model.get('result'), hits;
                if(result) {
                    var sources = result.get('sources');
                    if(result.get('hits')) {
                        hits = result.get('hits');
                    }
                    if(sources) {
                        sources.forEach(function(source) {
                            if(!source.get('done')) {
                                working = true;
                            }
                        });
                    }
                }
                return _.extend(this.model.toJSON(), {working: working, editing: this.editing, hits: hits});
            },
            editMode: function() {
                this.editing = true;
                this.render();
            },
            doneMode: function() {
                this.editing = false;
                this.render();
            },
            editSearch: function() {
                wreqr.vent.trigger('workspace:searchedit', dir.forward, this.model);
            },
            removeSearch: function() {
                this.model.collection.remove(this.model);
            },
            showSearch: function() {
                if(!this.editing) {
                    var progressFunction = function (value, model) {
                        model.mergeLatest();
                        wreqr.vent.trigger('map:clear');
                        wreqr.vent.trigger('map:results', model, false);
                    };
                    this.model.startSearch(progressFunction);

                    wreqr.vent.trigger('workspace:results', dir.forward, this.model);
                }
            }
        });

        WorkspaceView.MetacardList = Marionette.CollectionView.extend({
            itemView : WorkspaceView.MetacardItem,
            tagName: 'table',
            className: 'table'
        });

        WorkspaceView.SearchList = Marionette.CollectionView.extend({
            itemView : WorkspaceView.SearchItem,
            tagName: 'table',
            className: 'table'
        });

        WorkspaceView.Workspace = Marionette.Layout.extend({
            template: 'workspace',
            className: 'nav-list',
            regions: {
                workspaceSearchPanelRegion: '#workspaceSearchPanel',
                workspaceMetacardPanelRegion: '#workspaceMetacardPanel'
            },
            events: {
                'click #addSearch': 'addSearch'
            },
            addSearch: function() {
                var model = new QueryModel.Model();
                this.model.get('searches').add(model);
                wreqr.vent.trigger('workspace:searchedit', dir.forward, model);
            },
            onRender: function() {
                this.workspaceSearchPanelRegion.show(new WorkspaceView.SearchList({collection: this.model.get('searches')}));
                this.workspaceMetacardPanelRegion.show(new WorkspaceView.MetacardList({collection: this.model.get('metacards')}));
            }
        });

        WorkspaceView.WorkspaceItem = Marionette.ItemView.extend({
            template: 'workspaceItem',
            tagName: 'tr',
            className: 'workspace-row',
            events: {
                'click .workspace-remove': 'removeWorkspace',
                'click': 'showWorkspace'
            },
            modelEvents: {
                'change': 'render'
            },
            editing: false,
            initialize: function() {
                var view = this;
                wreqr.vent.on('workspace:editall', _.bind(this.editMode, this));
                wreqr.vent.on('workspace:saveall', _.bind(this.doneMode, this));
                if(this.model.get('searches')) {
                    var searches = this.model.get('searches');
                    searches.forEach(function(search) {
                        var result = search.get('result');
                        if(result) {
                            view.listenTo(result, 'change', view.render);
                        }
                    });
                }
                if(this.model.get('metacards')) {
                    this.listenTo(this.model.get('metacards'), 'change', this.render);
                }
            },
            serializeData: function() {
                var working = false, hits, searches = this.model.get('searches');
                if(searches) {
                    searches.forEach(function(search) {
                        var result = search.get('result');
                        if(result) {
                            if(result.get('hits')) {
                                if(!hits) hits = 0;
                                hits += result.get('hits');
                            }
                            var sources = result.get('sources');
                            if(sources) {
                                sources.forEach(function(source) {
                                    if(!source.get('done')) {
                                        working = true;
                                    }
                                });
                            }
                        }
                    });
                }
                return _.extend(this.model.toJSON(), {working: working, hits: hits, editing: this.editing});
            },
            editMode: function() {
                this.editing = true;
                this.render();
            },
            doneMode: function() {
                this.editing = false;
                this.render();
            },
            showWorkspace: function() {
                if(!this.editing) {
                    var view = this;
                    wreqr.reqres.setHandler('workspace:getCurrent', function () {
                        return view.model;
                    });
                    wreqr.vent.trigger('workspace:show', dir.forward, this.model);
                }
            },
            removeWorkspace: function() {
                this.model.collection.remove(this.model);
            }
        });

        WorkspaceView.WorkspaceList = Marionette.CollectionView.extend({
            template: 'workspaceList',
            itemView : WorkspaceView.WorkspaceItem,
            tagName: 'table',
            className: 'table workspace-table'
        });

        WorkspaceView.WorkspacesLayoutView = Marionette.Layout.extend({
            template: 'workspaceList',
            className: 'height-full',
            regions: {
                workspaceControlRegion: '#workspaceControl',
                workspaceRegion: {
                    selector: "#workspace",
                    regionType:  SlidingRegion
                }
            },

            modelEvents: {
                'change': 'render'
            },

            initialize: function() {
                _.bindAll(this);

                wreqr.vent.on('workspace:tabshown', _.bind(this.setupEvents, this));
            },

            setupEvents: function(tabHash) {
                if(this.currentSearch && tabHash === '#workspaces') {
                    wreqr.vent.trigger('map:clear');
                    wreqr.vent.trigger('map:results', this.currentSearch.get('result'), false);

                    if(this.currentSearch) {
                        this.updateMapPrimitive();
                    }
                }

                if(tabHash === '#workspaces') {
                    wreqr.vent.on('workspace:show', this.showWorkspace);
                    wreqr.vent.on('workspace:results', this.showWorkspaceResults);
                    wreqr.vent.on('metacard:selected', this.showWorkspaceMetacard);
                    wreqr.vent.on('workspace:new', this.showWorkspaceAdd);
                    wreqr.vent.on('workspace:searchedit', this.showWorkspaceSearchEdit);
                    wreqr.vent.on('workspace:list', this.showWorkspaceList);
                    wreqr.vent.on('workspace:save', this.workspaceSave);
                    wreqr.vent.on('workspace:saveall', this.workspaceSave);
                    wreqr.vent.on('workspace:cancel', this.workspaceCancel);
                    this.workspaceRegion.show(undefined, dir.none);
                } else {
                    wreqr.vent.off('workspace:show', this.showWorkspace);
                    wreqr.vent.off('workspace:results', this.showWorkspaceResults);
                    wreqr.vent.off('metacard:selected', this.showWorkspaceMetacard);
                    wreqr.vent.off('workspace:new', this.showWorkspaceAdd);
                    wreqr.vent.off('workspace:searchedit', this.showWorkspaceSearchEdit);
                    wreqr.vent.off('workspace:list', this.showWorkspaceList);
                    wreqr.vent.off('workspace:save', this.workspaceSave);
                    wreqr.vent.off('workspace:saveall', this.workspaceSave);
                    wreqr.vent.off('workspace:cancel', this.workspaceCancel);
                    this.workspaceRegion.close();
                }
            },

            workspaceCancel: function(model) {
                if(model && !model.get('name')) {
                    if(this.currentWorkspace) {
                        this.currentWorkspace.get('searches').remove(model);
                    }
                }
                wreqr.vent.trigger('workspace:show', dir.backward, this.currentWorkspace);
            },

            workspaceSave: function() {
                var view = this;
                if(view.currentWorkspace) {
                    wreqr.reqres.setHandler('workspace:getCurrent', function () {
                        return view.currentWorkspace;
                    });
                }
                this.model.save();
            },

            showWorkspace: function(direction, model) {
                if(model) {
                    this.currentWorkspace = model;
                }
                this.workspaceRegion.show(new WorkspaceView.Workspace({model: this.currentWorkspace}), direction);
            },

            showWorkspaceList: function(direction) {
                this.workspaceRegion.show(new WorkspaceView.WorkspaceList({collection: this.model.get('workspaces')}), direction);
            },

            showWorkspaceSearchEdit: function(direction, model) {
                this.workspaceRegion.show(new QueryView.QueryView({isWorkspace: true, model: model ? model : new QueryModel.Model()}), direction);
            },

            showWorkspaceAdd: function(direction) {
                this.workspaceRegion.show(new WorkspaceView.WorkspaceAdd({collection: this.model.get('workspaces')}), direction);
            },

            showWorkspaceResults: function(direction, model) {
                if(model) {
                    this.currentSearch = model;
                }

                this.updateMapPrimitive();

                this.workspaceRegion.show(new MetacardList.MetacardListView({model: this.currentSearch.get('result')}), direction);
                wreqr.vent.trigger('map:clear');
                wreqr.vent.trigger('map:results', this.currentSearch.get('result'));
            },

            showWorkspaceMetacard: function(model) {
                var direction;
                if(model.get('direction')) {
                    direction = model.get('direction');
                } else {
                    direction = dir.forward;
                }
                this.workspaceRegion.show(new MetacardDetail.MetacardDetailView({model: model}), direction);
            },

            updateMapPrimitive: function() {
                wreqr.vent.trigger('search:drawend');
                if(this.currentSearch.get('north') && this.currentSearch.get('south') && this.currentSearch.get('east') &&
                    this.currentSearch.get('west')) {
                    wreqr.vent.trigger('search:bboxdisplay', this.currentSearch);
                    this.currentSearch.trigger('EndExtent');
                } else if(this.currentSearch.get('lat') && this.currentSearch.get('lon') && this.currentSearch.get('radius')) {
                    wreqr.vent.trigger('search:circledisplay', this.currentSearch);
                    this.currentSearch.trigger('EndExtent');
                }
            },

            onRender: function() {
                this.workspaceControlRegion.show(new WorkspaceControl.WorkspaceControlView());
                this.workspaceRegion.show(new WorkspaceView.WorkspaceList({collection: this.model.get('workspaces')}));
            }
        });

        WorkspaceView.WorkspaceLayout = Marionette.Layout.extend({
            template : 'workspacePanel',
            className: 'partialaffix span3 row-fluid nav nav-list well well-small search-controls',
            regions : {
                workspacesRegion: "#workspaces",
                searchRegion: "#search"
            },

            events: {
                'shown.bs.tab .tabs-below>.nav-tabs>li>a': 'tabShown'
            },

            tabShown: function(e) {
                wreqr.vent.trigger('workspace:tabshown', e.target.hash);
            },

            onRender : function(){
                this.workspacesRegion.show(new WorkspaceView.WorkspacesLayoutView({model: this.model}));
                this.searchRegion.show(new Search.SearchLayout());

                if(maptype.isNone()) {
                    this.$el.addClass('full-screen-search');
                }

                wreqr.vent.trigger('workspace:tabshown', this.$('.nav-tabs > .active a').attr('href'));
            }
        });

        return WorkspaceView;
    });
