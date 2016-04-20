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
    'text!./workspace-selector.hbs',
    'js/CustomElements',
    'js/store',
    'component/confirmation/confirmation.view',
    'js/router'
], function (Marionette, _, $, workspaceSelectorTemplate, CustomElements, store, ConfirmationView,
        router) {

    function getWorkspaceId(){
        return store.get('componentWorkspaces').getWorkspaceId();
    }

    function setWorkspaceId(workspaceId){
        store.get('componentWorkspaces').setWorkspaceId(workspaceId);
    }

    function turnOnEditing(){
        store.get('componentWorkspaces').turnOnEditing();
    }

    function turnOffEditing(){
        store.get('componentWorkspaces').turnOffEditing();
    }

    var WorkspaceSelector = Marionette.LayoutView.extend({
        setDefaultModel: function(){
            this.model = store.get('workspaces');
        },
        template: workspaceSelectorTemplate,
        tagName: CustomElements.register('workspace-selector'),
        modelEvents: {
        },
        events: {
            'click .workspaces-list .workspace': 'clickWorkspace',
            'click .workspaces-add': 'createWorkspace',
            'click .workspace-delete': 'deleteWorkspace',
            'dblclick .workspaces-list .workspace': 'openWorkspace'
        },
        ui: {
            workspaceList: '.workspaces-list'
        },
        regions: {
        },
        initialize: function(options){
            if (options.model === undefined){
                this.setDefaultModel();
            }
            this.listenTo(this.model, 'all', this.rerender);
        },
        rerender: function(){
            this.render();
        },
        onRender: function(){
            this.highlightSelectedWorkspace();
        },
        onAttach: function(){
            this.highlightSelectedWorkspace();
            this.scrollToSelectedWorkspace();
        },
        clickWorkspace: function(event){
            event.stopPropagation();
            turnOffEditing();
            var workspace = event.currentTarget;
            setWorkspaceId(workspace.getAttribute('data-id'));
            this.changeWorkspace();
        },
        changeWorkspace: function(event){
            this.highlightSelectedWorkspace();
        },
        highlightSelectedWorkspace: function(){
            this.$el.find('.workspaces-list .workspace').removeClass('is-selected');
            this.$el.find('.workspaces-list .workspace[data-id='+getWorkspaceId()+']').addClass('is-selected');
        },
        createWorkspace: function(){
            turnOnEditing();
            setWorkspaceId(this.model.createWorkspace());
            this.scrollToSelectedWorkspace();
            this.changeWorkspace();
        },
        deleteWorkspace: function(event){
            event.stopPropagation();
            var workspace = event.currentTarget.getAttribute('data-id');
            this.listenTo(ConfirmationView.generateConfirmation({
                    prompt: 'Are you sure you want to delete this workspace?',
                    yes: 'Delete',
                    no: 'Cancel'
                }),
                'change:choice',
                function(model, value){
                    this.handleChoice(value, workspace);
                });
        },
        handleChoice: function(shouldDelete, workspace){
            if (shouldDelete){
                setWorkspaceId(undefined);
                this.model.get(workspace).destroy();
            } else {

            }
        },
        scrollToSelectedWorkspace: function(){
            var selectedWorkspace = this.$el.find('[data-id='+getWorkspaceId()+']')[0];
            if (selectedWorkspace!==undefined){
                selectedWorkspace.scrollIntoView();
            }
        },
        openWorkspace: function(){
            router.navigate('workspaces/'+getWorkspaceId(), {trigger: true, replace: true});
        }
    });

    return WorkspaceSelector;
});
