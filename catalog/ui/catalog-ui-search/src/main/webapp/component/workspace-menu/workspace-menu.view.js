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
    './workspace-menu.hbs',
    'js/CustomElements',
    'component/content-title/content-title.view',
    'component/dropdown/dropdown',
    'component/dropdown/workspace-interactions/dropdown.workspace-interactions.view',
    'js/store',
], function (Marionette, _, $, template, CustomElements, TitleView, DropdownModel, 
    WorkspaceInteractionsView, store) {

    return Marionette.LayoutView.extend({
        setDefaultModel: function(){
            this.model = store.get('content');
        },
        template: template,
        tagName: CustomElements.register('workspace-menu'),
        regions: {
            title: '.content-title',
            workspaceInteractions: '.content-interactions'
        },
        initialize: function (options) {
            if (options.model === undefined){
                this.setDefaultModel();
            }
            this.listenTo(this.model, 'change:currentWorkspace', this.updateWorkspaceInteractions);
            this.listenTo(this.model, 'change:currentWorkspace', this.handleWorkspaceChange);
        },
        onBeforeShow: function(){
            this.title.show(new TitleView());
            this.updateWorkspaceInteractions();
        },
        updateWorkspaceInteractions: function(workspace) {
            if (workspace && workspace.changed.currentWorkspace) {
                this.workspaceInteractions.show(new WorkspaceInteractionsView({
                    model: new DropdownModel(),
                    modelForComponent: this.model.get('currentWorkspace')
                }));
            }
        }
    });
});
