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
    'text!./home-items.hbs',
    'js/CustomElements',
    'js/store',
    'js/router',
    'moment',
    'component/lightbox/lightbox.view.instance',
    'component/tabs/workspace/tabs-workspace',
    'component/tabs/workspace/tabs-workspace.view'
], function (wreqr, Marionette, _, $, template, CustomElements, store, router, moment, lightboxInstance,
    TabsModel, TabsView) {

    return Marionette.ItemView.extend({
        setDefaultModel: function(){
            this.model = store.get('workspaces');
        },
        template: template,
        tagName: CustomElements.register('home-items'),
        modelEvents: {
        },
        events: {
            'click .choice': 'handleChoice',
            'click .choice-actions': 'editWorkspaceDetails'
        },
        ui: {
        },
        initialize: function(options){
            if (!options.model){
                this.setDefaultModel();
            }
            this.listenTo(this.model, 'all', _.throttle(this.render, 200));
        },
        onRender: function(){
        },
        handleChoice: function(event){
            var workspaceId = $(event.currentTarget).attr('data-workspaceId');
            router.navigate('workspaces/'+workspaceId, {trigger: true});
        },
        editWorkspaceDetails: function(event){
            event.stopPropagation();
            var workspaceId = $(event.currentTarget).attr('data-workspaceId');
            lightboxInstance.model.updateTitle('Workspace Details');
            lightboxInstance.model.open();
            lightboxInstance.lightboxContent.show(new TabsView({
                model: new TabsModel({
                    workspaceId: workspaceId
                })
            }));
        },
        serializeData: function() {
            var workspacesJSON = this.model.toJSON();
            workspacesJSON.forEach(function(workspace){
                workspace.previewImage = workspace.metacards[0];
                workspace.niceDate = moment(workspace.modified).fromNow();
            });
            return workspacesJSON;
        }
    });
});
