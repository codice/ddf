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
    './metacard-title.hbs',
    'js/CustomElements',
    'js/IconHelper',
    'js/store',
    'component/dropdown/popout/dropdown.popout.view',
    'component/metacard-interactions/metacard-interactions.view'
], function (wreqr, Marionette, _, $, template, CustomElements, IconHelper, store,
            PopoutView, MetacardInteractionsView) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('metacard-title'),
        modelEvents: {
        },
        events: {
            'click .metacard-save': 'handleSave',
            'click .metacard-unsave': 'handleUnsave'
        },
        ui: {
        },
        regions: {
            metacardInteractions: '.metacard-interactions'
        },
        onBeforeShow: function(){
            this.metacardInteractions.show(PopoutView.createSimpleDropdown({
                componentToShow: MetacardInteractionsView,
                dropdownCompanionBehaviors: {
                    navigation: {}
                },
                modelForComponent: this.model,
                leftIcon: 'fa fa-ellipsis-v'
            }));
        },
        initialize: function(){
            if (this.model.length === 1){
                this.listenTo(this.model.first().get('metacard').get('properties'), 'change', this.handleModelUpdates);
            }
            var currentWorkspace = store.getCurrentWorkspace();
            if (currentWorkspace) {
                this.listenTo(currentWorkspace, 'change:metacards', this.handleModelUpdates);
            }
            this.checkTags();
            this.checkIfSaved();
            this.checkIsInWorkspace();
        },
        handleModelUpdates: function(){
            var currentWorkspace = store.getCurrentWorkspace();
            if (currentWorkspace) {
                this.stopListening(currentWorkspace);
                this.listenTo(currentWorkspace, 'change:metacards', this.handleModelUpdates);
            }
            this.render();
            this.onBeforeShow();
            this.checkIfSaved();
            this.checkTags();
            this.checkIsInWorkspace();
        },
        serializeData: function(){
            var title, icon;
            if (this.model.length === 1){
                icon = IconHelper.getClass(this.model.first());
                title = this.model.first().get('metacard').get('properties').get('title');
            } else {
                title = this.model.length + ' Items';
            }
            return {
                title: title,
                icon: icon
            };
        },
        checkIfSaved: function(){
            var currentWorkspace = store.getCurrentWorkspace();
            if (currentWorkspace){
                var ids = this.model.map(function(result){
                    return result.get('metacard').get('properties').get('id');
                });
                var isSaved = true;
                ids.forEach(function(id){
                    if (currentWorkspace.get('metacards').indexOf(id) === -1){
                        isSaved = false;
                    }
                });
                this.$el.toggleClass('is-saved', isSaved);
            }
        },
        checkIsInWorkspace: function(){
            var currentWorkspace = store.getCurrentWorkspace();
            this.$el.toggleClass('in-workspace', Boolean(currentWorkspace));
        },
        checkTags: function(){
            var types = {};
            this.model.forEach(function(result){
                var tags = result.get('metacard').get('properties').get('metacard-tags');
                if (result.isWorkspace()){
                    types.workspace = true;
                } else if (result.isResource()){
                    types.resource = true;
                } else if (result.isRevision()){
                    types.revision = true;
                } else if (result.isDeleted()) {
                    types.deleted = true;
                }
                if (result.isRemote()){
                    types.remote = true;
                }
            });

            this.$el.toggleClass('is-mixed', Object.keys(types).length > 1);
            this.$el.toggleClass('is-workspace', types.workspace !== undefined);
            this.$el.toggleClass('is-resource', types.resource !== undefined);
            this.$el.toggleClass('is-revision', types.revision !== undefined);
            this.$el.toggleClass('is-deleted', types.deleted !== undefined);
            this.$el.toggleClass('is-remote', types.remote !== undefined);
        },
        handleSave: function(){
            var currentWorkspace = store.getCurrentWorkspace();
            if (currentWorkspace){
                var ids = this.model.map(function(result){
                    return result.get('metacard').get('properties').get('id');
                });
                currentWorkspace.set('metacards', _.union(currentWorkspace.get('metacards'), ids));
            }
            this.checkIfSaved();
        },
        handleUnsave: function(){
            var currentWorkspace = store.getCurrentWorkspace();
            if (currentWorkspace){
                var ids = this.model.map(function(result){
                    return result.get('metacard').get('properties').get('id');
                });
                currentWorkspace.set('metacards', _.difference(currentWorkspace.get('metacards'), ids));
            }
            this.checkIfSaved();
        }
    });
});
