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
    'marionette',
    'underscore',
    'jquery',
    'text!./workspaces.hbs',
    'js/CustomElements',
    'component/tabs/workspace/tabs-workspace',
    'component/tabs/workspace/tabs-workspace.view',
    'component/workspace-selector/workspace-selector.view',
    'js/store'
], function (Marionette, _, $, workspacesTemplate, CustomElements, TabsModel, TabsView,
             WorkspaceSelectorView, store) {

    var Workspaces = Marionette.LayoutView.extend({
        template: workspacesTemplate,
        tagName: CustomElements.register('workspaces'),
        modelEvents: {
            'change:workspaceId': 'changeWorkspace'
        },
        events: {
            'click .workspaces-details': 'shiftRight',
            'click .workspaces-selector': 'shiftLeft'
        },
        ui: {
        },
        regions: {
            'workspaceDetails': '.workspaces-details',
            'workspaceSelector': '.workspaces-selector'
        },
        initialize: function(){
        },
        onRender: function(){
            var workspaceSelectorView = new WorkspaceSelectorView({
                model: store.get('workspaces')
            });
            this.workspaceSelector.show(workspaceSelectorView);
            this.changeWorkspace();
        },
        changeWorkspace: function(){
            var workspaceId = this.model.get('workspaceId');
            if (workspaceId === undefined){
                this.workspaceDetails.empty();
            } else {
                this.shiftRight();
                this.workspaceDetails.show(new TabsView({
                    model: new TabsModel({
                        workspaceId: this.model.get('workspaceId')
                    })
                }));
            }
        },
        shiftLeft: function(event){
            this.$el.addClass('shifted-left');
            this.$el.removeClass('shifted-right');
        },
        shiftRight: function(){
            this.$el.addClass('shifted-right');
            this.$el.removeClass('shifted-left');
        }
    });

    return Workspaces;
});
