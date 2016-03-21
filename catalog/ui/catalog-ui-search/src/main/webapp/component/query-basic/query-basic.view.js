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
    'text!./query-basic.hbs',
    'js/CustomElements',
    'js/store',
    'component/input/input.collection',
    'component/input/input.collection.view',
    'component/input/query-time/input-query-time',
    'component/input/query-time/input-query-time.view'
], function (Marionette, _, $, queryBasicTemplate, CustomElements, store, InputCollection, InputCollectionView, InputQueryTime, InputQueryTimeView) {

    function isEditing(){
        return store.get('content').isEditing();
    }

    function turnOffEditing(){
        store.get('content').turnOffEditing();
    }

    var QueryBasic = Marionette.LayoutView.extend({
        template: queryBasicTemplate,
        tagName: CustomElements.register('query-basic'),
        modelEvents: {
        },
        events: {
            'click .queryBasic-edit': 'turnOnEdit',
            'click .queryBasic-cancel': 'revert',
            'click .queryBasic-save': 'save'
        },
        regions: {
            queryBasicInputs: '.queryBasic-inputs'
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
            time: {
                type: InputQueryTime,
                properties: {
                    label: 'Time',
                    validation: undefined,
                    description: 'Search based on relative or absolute time of the created, modified, or effective date.',
                    readOnly: false,
                    value: '',
                    id: 'time'
                }
            }
        },
        onBeforeShow: function(){
            this.queryBasicInputs.show(this._inputCollectionView);
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
                this._inputCollection.add(new properties[property].type(properties[property].properties))
            }
        },
        addStringProperty: function(property, attributes){
            var propertyModel = new Input(attributes);
            this._inputCollection.add(propertyModel);
        },
        addDateProperty: function(property, attributes){
            this.addStringProperty(property,attributes);
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

    return QueryBasic;
});
