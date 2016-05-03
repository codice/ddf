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
    'text!./workspace-item.hbs',
    'js/CustomElements',
    'js/store',
    'wreqr',
    'component/menu-vertical/popout/menu-vertical.popout.view',
    'component/content-toolbar/content-toolbar',
    'moment'
], function (wreqr, Marionette, _, $, template, CustomElements, store, wreqr, MenuView, ContentToolbar,
             moment) {

    return Marionette.ItemView.extend({
        template: template,
        tagName: CustomElements.register('workspace-item'),
        modelEvents: {
        },
        events: {
            'click .choice': 'handleChoice',
            'click .choice-actions': 'editWorkspaceDetails'
        },
        ui: {
        },
        initialize: function(options){
            this.listenTo(this.model, 'all', _.throttle(function(){
                if (!this.isDestroyed){
                    this.render();
                }
            }.bind(this), 200));
            this._workspaceMenuModel = new ContentToolbar();
        },
        initializeMenus: function(){
            this._workspaceMenu = MenuView.getNewWorkspaceMenu(this._workspaceMenuModel,
                function(){
                    switch(this.displayType){
                        case 'Grid':
                            return this.el.querySelector('.choice.as-grid .actions-icon');
                            break;
                        case 'List':
                            return this.el.querySelector('.choice.as-list .actions-icon');
                            break;
                    }
                }.bind(this),
                'workspace',
                this.model);
        },
        firstRender: true,
        onRender: function(){
            if (this.firstRender){
                this.firstRender = false;
                this.initializeMenus();
            }
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
        editWorkspaceDetails: function(event){
            event.stopPropagation();
            this._workspaceMenuModel.activate('workspace');
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
