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
    'wreqr',
    'text!templates/workspace/workspaceSaveResults.handlebars',
    // Load non attached libs and plugins
    'perfectscrollbar'
], function ($, _, Marionette, Workspace, Backbone, dir, wreqr, workspaceSaveResults) {
    'use strict';
    var SaveResults = Marionette.ItemView.extend({
        template: workspaceSaveResults,
        events: {
            'click .submit': 'saveResults',
            'keypress input[name=workspaceName]': 'saveResultsOnEnter',
            'click #cancel': 'cancel',
            'click input[name=workspaceName]': 'selectNewWorkspace'
        },
        initialize: function (options) {
            this.search = options.search;
            this.records = options.records;
            this.viewModel = new Backbone.Model();
            this.modelBinder = new Backbone.ModelBinder();
        },
        serializeData: function () {
            return _.extend(this.model.toJSON(), { hasSearch: this.search ? true : false });
        },
        onRender: function () {
            var modelBindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            this.modelBinder.bind(this.viewModel, this.$el, modelBindings, {
                changeTriggers: {
                    '': 'change',
                    'input[name=workspaceName]': 'input'
                }
            });
        },
        onShow: function () {
            this.$('input[name=workspaceName]').val('New Workspace');
            if (this.search) {
                this.$('input[name=searchName]').focus();
            } else {
                this.$('input[name=workspaceName]').focus();
            }
        },
        saveResults: function () {
            var workspace;
            if (!this.viewModel.get('workspaceType')) {
                return;
            } else {
                workspace = this.model.get('workspaces').findWhere({ name: this.viewModel.get('workspaceType') });
            }
            if (this.viewModel.get('workspaceType') === 'create_new_workspace') {
                if (!this.viewModel.get('workspaceName') || this.viewModel.get('workspaceName') === '') {
                    this.$('input[name=workspaceName]').focus();
                    return;
                }
                workspace = new Workspace.Model({ name: this.viewModel.get('workspaceName') });
                this.model.get('workspaces').add(workspace);
            }
            if (this.records) {
                workspace.get('metacards').add(this.records);
            }
            if (this.search) {
                if (!this.viewModel.get('searchName')) {
                    this.$('input[name=searchName]').focus();
                    return;
                }
                this.search.set({ name: this.viewModel.get('searchName') });
                workspace.get('searches').add(this.search);
            }
            wreqr.vent.trigger('workspace:saveall');
            this.cancel();
        },
        saveResultsOnEnter: function (e) {
            if (e.keyCode === 13) {
                this.saveResults();
                e.preventDefault();
            } else {
                this.selectNewWorkspace();
            }
        },
        selectNewWorkspace: function (e) {
            if (e) {
                if (this.$('input[name=workspaceName]').val() === 'New Workspace') {
                    this.$('input[name=workspaceName]').val('');
                }
            }
            this.$('#workspaceNameRadio').click();
        },
        onDestroy: function () {
            this.search = undefined;
            this.records = undefined;
            this.workspace = undefined;
            this.modelBinder.unbind();
        },
        cancel: function () {
            if (this.search) {
                wreqr.vent.trigger('workspace:searchsavecancel');
            }
            if (this.records) {
                wreqr.vent.trigger('workspace:resultssavecancel');
            }
        }
    });
    return SaveResults;
});
