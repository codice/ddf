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
    './workspace-item.hbs',
    'js/CustomElements',
    'js/store',
    'moment',
    'component/dropdown/dropdown',
    'component/dropdown/workspace-interactions/dropdown.workspace-interactions.view',
], function (wreqr, Marionette, _, $, template, CustomElements, store, moment, DropdownModel, WorkspaceInteractionsDropdownView) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('workspace-item'),
        regions: {
            gridWorkspaceActions: '.as-grid .choice-actions',
            listWorkspaceActions: '.as-list .choice-actions'
        },
        modelEvents: {
        },
        events: {
            'click .choice': 'handleChoice'
        },
        ui: {
        },
        initialize: function(options){
            this.listenTo(this.model, 'all', _.throttle(function(){
                if (!this.isDestroyed){
                    this.render();
                }
            }.bind(this), 200));
        },
        firstRender: true,
        onRender: function(){
            this.gridWorkspaceActions.show(new WorkspaceInteractionsDropdownView({
                model: new DropdownModel(),
                modelForComponent: this.model
            }));
            this.listWorkspaceActions.show(new WorkspaceInteractionsDropdownView({
                model: new DropdownModel(),
                modelForComponent: this.model
            }));
        },
        handleChoice: function(event){
            var workspaceId = $(event.currentTarget).attr('data-workspaceId');
            wreqr.vent.trigger('router:navigate', {
                fragment: 'workspaces/'+workspaceId,
                options: {
                    trigger: true
                }
            });
        },
        serializeData: function() {
            var workspacesJSON = this.model.toJSON();
            workspacesJSON.previewImage = workspacesJSON.metacards[0];
            workspacesJSON.niceDate = moment(workspacesJSON.modified).fromNow();
            return workspacesJSON;
        },
        activateGridDisplay: function(){
            this.displayType = 'Grid';
            this.$el.addClass('as-grid').removeClass('as-list');
        },
        activateListDisplay: function(){
            this.displayType = 'List';
            this.$el.addClass('as-list').removeClass('as-grid');
        },
        displayType: undefined
    });
});
