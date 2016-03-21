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
    'text!./workspace-basic.hbs',
    'js/CustomElements',
    'component/input/input',
    'component/input/input.view',
    'component/input/input.collection',
    'component/input/input.collection.view',
    'js/store'
], function (Marionette, _, $, workspaceBasicTemplate, CustomElements, Input, InputView,
             InputCollection, InputCollectionView, store) {

    function isEditing(){
        return store.get('componentWorkspaces').isEditing();
    }

    function turnOffEditing(){
        store.get('componentWorkspaces').turnOffEditing();
    }

    var WorkspaceBasic = Marionette.LayoutView.extend({
        template: workspaceBasicTemplate,
        tagName: CustomElements.register('workspace-basic'),
        modelEvents: {
        },
        events: {
            'click .workspaceBasic-edit': 'turnOnEdit',
            'click .workspaceBasic-cancel': 'revert',
            'click .workspaceBasic-save': 'save'
        },
        regions: {
            workspaceBasicInputs: '.workspaceBasic-inputs'
        },
        initialize: function () {
            this._inputCollection = new InputCollection();
            this._inputCollectionView = new InputCollectionView({
                collection: this._inputCollection
            });
            this.addPropertyViews();
        },
        _inputCollectionView: undefined,
        _inputCollection: undefined,
        properties: {
            name: {
                label: 'Name',
                validation: undefined,
                description: 'Workspace Name',
                readOnly: false,
                value: '',
                id: 'name'
            },
            createdDate: {
                label: 'Created Date',
                validation: undefined,
                description: 'The date the workspace was created.',
                readOnly: true,
                value: '',
                id: 'createdDate'
            },
            lastModifiedDate: {
                label: 'Last Modified Date',
                validation: undefined,
                description: 'The last date the workspace was modified.',
                readOnly: true,
                value: '',
                id: 'lastModifiedDate'
            }
        },
        onBeforeShow: function(){
            this.workspaceBasicInputs.show(this._inputCollectionView);
        },
        onAttach: function () {
            if (isEditing()){
                this.turnOnEdit();
            }
        },
        addPropertyViews: function(){
            var view = this;
            var properties = this.properties;
            for (var property in properties){
                properties[property].value = this.model.get(property);
                view.addStringProperty(property,properties[property]);
            }
        },
        addStringProperty: function(property, attributes){
            var propertyModel = new Input(attributes);
            this._inputCollection.add(propertyModel);
        },
        turnOnEdit: function(){
            this.$el.addClass('is-editing');
            this._inputCollectionView.turnOnEditing();
            this._inputCollectionView.focus();
        },
        turnOffEdit: function(){
            this.$el.removeClass('is-editing');
            this._inputCollectionView.turnOffEditing();
        },
        revert: function(){
            this._inputCollectionView.revert();
            this.turnOffEdit();
        },
        save: function(){
            this._inputCollectionView.save();
            this.turnOffEdit();
            this.model.set(this._inputCollection.getEditableValues());
        },
        _editMode: false
    });

    return WorkspaceBasic;
});
