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
var filter = '';

function convertArrayToModels(array){
    return array.map((key) => {
        return {
            id: key
        };
    });
}

function getDifference(collection, array){
    return collection.filter((model) => array.indexOf(model.id) === -1);
}

function intersect(collection, array){
    var difference = getDifference(collection, array);
    collection.remove(difference);
    return difference;
}

function sync(collection, array){
    var difference = getDifference(collection, array);
    collection.remove(difference);
    collection.add(convertArrayToModels(array));
    return difference;
}

define([
    'backbone',
    'marionette',
    'underscore',
    'jquery',
    './editor.hbs',
    'js/CustomElements',
    'component/property/property.view',
    'component/property/property',
    'component/dropdown/details-filter/dropdown.details-filter.view',
    'component/dropdown/dropdown',
    'component/details-interactions/details-interactions.view',
    'component/dropdown/popout/dropdown.popout.view',
    'component/singletons/user-instance',
    'properties'
], function (Backbone, Marionette, _, $, template, CustomElements, PropertyView, Property, DetailsFilterView, DropdownModel, DetailsInteractionsView,
    PopoutView, user, properties) {

    return Marionette.LayoutView.extend({
        setDefaultModel: function(){
            //override
        },
        template: template,
        tagName: CustomElements.register('editor'),
        modelEvents: {
        },
        events: {
            'click .editor-edit': 'edit',
            'click .editor-save': 'save',
            'click .editor-cancel': 'cancel'
        },
        regions: {
            editorProperties: '> .editor-properties',
            editorFilter: '> .editor-header > .header-filter',
            editorActions: '> .editor-header > .header-actions'
        },
        attributesAdded: undefined,
        attributesRemoved: undefined,
        attributesMocked: undefined,
        attributesToKeep: undefined,
        initialize: function (options) {
            if (options.model === undefined){
                this.setDefaultModel();
            }
            this.handleTypes();
            this.attributesAdded = new Backbone.Collection([]);
            this.attributesRemoved = new Backbone.Collection([]);
            this.attributesMocked = new Backbone.Collection([]);
            this.attributesToKeep = new Backbone.Collection([]);
            this.listenTo(this.attributesAdded, 'reset', this.handleEphemeralReset);
            this.listenTo(this.attributesRemoved, 'reset', this.handleAttributesToRemoveReset);
            //this.listenTo(user.get('user').get('preferences'), 'change:columnOrder', this.render);
            this.listenTo(user.get('user').get('preferences'), 'change:inspector-summaryShown', this.handleFilterValue);
            this.listenTo(user.get('user').get('preferences'), 'change:inspector-detailsHidden', this.handleFilterValue);
        },
        handleTypes: function(){
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
        getEditorActionsOptions: function(){
            return {
                summary: true
            };
        },
        generateEditorActions: function(){
            this.editorActions.show(PopoutView.createSimpleDropdown(_.extend({
                componentToShow: DetailsInteractionsView,
                dropdownCompanionBehaviors: {
                    navigation: {}
                },
                modelForComponent: this.model,
                label: 'Actions',
                rightIcon: 'fa fa-ellipsis-v',
                selectionInterface: this.selectionInterface,
                options: _.extend({
                    selectionInterface: this.selectionInterface
                }, this.getEditorActionsOptions())
            })));         
            this.listenTo(this.editorActions.currentView.model, 'change:attributesToAdd', this.handleAttributeAdd);
            this.listenTo(this.editorActions.currentView.model, 'change:attributesToRemove', this.handleAttributeRemove);
        },
        onBeforeShow: function(){
            this.editorFilter.show(new DetailsFilterView({
                model: new DropdownModel({
                    value: filter
                })
            }));
            this.listenTo(this.editorFilter.currentView.model, 'change:value', this.handleFilterValue);
            this.handleFilterValue();
            this.generateEditorActions();
        }, 
        handleAttributesToRemoveReset: function(collection, options){
            this.handleAttributesToRemove();
            var ephemeralAttributesToUnRemove = this.attributesMocked.map((model) => model.id)
                .filter((id) => this.attributesRemoved.get(id) === undefined);
            this.editorProperties.currentView.removeProperties(ephemeralAttributesToUnRemove);
            this.generateEditorActions();
        },
        handleEphemeralReset: function(collection, options){
            this.attributesToKeep.add(options.previousModels);
            var ephemeralAttributes = options.previousModels.map((model) => model.id);
            this.editorProperties.currentView.removeProperties(ephemeralAttributes);
            this.generateEditorActions();
        },
        handleAttributeRemove: function(){
            sync(this.attributesRemoved, this.editorActions.currentView.model.get('attributesToRemove')[0]);
            var newAttributes = this.editorProperties.currentView.addProperties(this.attributesRemoved.pluck('id'));
            this.attributesMocked.add(convertArrayToModels(newAttributes));
            this.editorProperties.currentView.removeProperties(intersect(this.attributesMocked, this.attributesRemoved.pluck('id')));
            this.handleNewProperties();
            this.handleAttributesToRemove();
        },
        handleAttributesToRemove: function(){
            this.editorProperties.currentView.children.forEach((propertyView) => {
                var id = propertyView.model.id;
                propertyView.$el.toggleClass('scheduled-for-removal', this.attributesRemoved.get(id) !== undefined);
            });
            this.handleFilterValue();
        },
        handleAttributesToAdd: function(){
            this.editorProperties.currentView.children.forEach((propertyView) => {
                var id = propertyView.model.id;
                propertyView.$el.toggleClass('scheduled-for-add', this.attributesAdded.get(id) !== undefined);
            });
        },
        handleAttributeAdd: function(){
            var difference = sync(this.attributesAdded, this.editorActions.currentView.model.get('attributesToAdd')[0]);
            var newAttributes = this.editorProperties.currentView.addProperties(this.attributesAdded.pluck('id'));
            this.editorProperties.currentView.removeProperties(difference);
            this.handleNewProperties();
            this.handleAttributesToAdd();
            this.handleFilterValue();
        },
        isSupposedToBeShown: function(attribute){
            var ephemeralAttributes = this.attributesAdded.map((model) => model.id);
            var attributesToRemove = this.attributesRemoved.map((model) => model.id);
            var attributesToKeep = this.attributesToKeep.map((model) => model.id);
            if (attributesToKeep.indexOf(attribute) >= 0 || ephemeralAttributes.indexOf(attribute) >= 0 || attributesToRemove.indexOf(attribute) >= 0) {
                return true;
            }
            if (this.getEditorActionsOptions().summary){
                var userSummaryChoice = user.get('user').get('preferences').get('inspector-summaryShown');
                if (userSummaryChoice.length > 0){
                    return userSummaryChoice.indexOf(attribute) >= 0;
                } else {
                    return properties.summaryShow.indexOf(attribute) >= 0;
                }
            } else {
                return user.get('user').get('preferences').get('inspector-detailsHidden').indexOf(attribute) === -1;
            }
        },
        handleFilterValue: function(){
            filter = this.editorFilter.currentView.model.get('value');
            this.editorProperties.currentView.children.forEach((propertyView) => {
                var identifier = propertyView.model.get('label') || propertyView.model.id;
                if (identifier.toLowerCase().indexOf(filter.toLowerCase()) >= 0 && this.isSupposedToBeShown(propertyView.model.id)){
                    propertyView.show();
                } else {
                    propertyView.hide();
                }
            });
        },
        handleNewProperties: function(){
            this.editorProperties.currentView.turnOnLimitedWidth();
            this.$el.addClass('is-editing');
            this.editorProperties.currentView.turnOnEditing();
        },
        edit: function(){
            this.$el.addClass('is-editing');
            this.editorProperties.currentView.turnOnEditing();
            this.editorProperties.currentView.focus();
        },
        cancel: function(){
            this.$el.removeClass('is-editing');
            this.attributesAdded.reset();
            this.attributesRemoved.reset();
            this.editorProperties.currentView.revert();
            this.editorProperties.currentView.turnOffEditing();
            this.afterCancel();
        },
        save: function(){
            this.$el.removeClass('is-editing');
            var ephemeralAttributes = this.attributesAdded.map((model) => model.id);
            var attributesToRemove = this.attributesRemoved.map((model) => model.id);
            this.afterSave(this.editorProperties.currentView.toPatchJSON(ephemeralAttributes, attributesToRemove));
            this.attributesAdded.reset();
            this.attributesRemoved.reset();
            this.editorProperties.currentView.revert();
            this.editorProperties.currentView.turnOffEditing();
        },
        afterCancel: function(){
            //override
        },
        afterSave: function(){
            //override
        },
        toJSON: function(){
            return this.editorProperties.currentView.toJSON();
        }
    });
});
