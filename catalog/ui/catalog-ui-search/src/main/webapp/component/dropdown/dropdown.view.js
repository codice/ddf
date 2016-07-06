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
    'js/CustomElements',
    './dropdown.companion.view',
    './dropdown',
    'text!./dropdown.hbs',
    'component/select/select.collection.view'
], function (Marionette, _, $, CustomElements, DropdownCompanionView, DropdownModel, template, SelectView) {

    return Marionette.LayoutView.extend({
        template: template,
        className: 'is-simpleDropdown',
        tagName: CustomElements.register('dropdown'),
        events: {
            'click': 'handleClick'
        },
        handleClick: function(e){
            e.preventDefault();
            e.stopPropagation();
            if (this.model.get('isEditing')){
                this.model.toggleOpen();
            }
        },
        handleEditing: function(){
            this.$el.toggleClass('is-editing', this.model.get('isEditing'));
        },
        hasTail: false,
        componentToShow: undefined,
        modelForComponent: undefined,
        dropdownCompanion: undefined,
        initializeComponentModel: function(){
            //override if you need more functionality
            this.modelForComponent = this.model;
        },
        getTargetElement: function(){
            //override with where you want the dropdown to center
        },
        listenToComponent: function(){
            //override if you need more functionality
            this.listenTo(this.modelForComponent, 'change:value', function(){
                this.model.set('value', this.modelForComponent.get('value'));
            }.bind(this));
        },
        initialize: function(){
            this.initializeComponentModel();
            this.listenTo(this.model, 'change:value', this.render);
            this.listenTo(this.model, 'change:isOpen', this.render);
            this.listenTo(this.model, 'change:isEditing', this.handleEditing);
            this.listenToComponent();
            this.handleEditing();
        },
        initializeDropdown: function(){
            this.dropdownCompanion = DropdownCompanionView.getNewCompanionView(this);
        },
        firstRender: true,
        onRender: function(){
            if (this.firstRender){
                this.firstRender = false;
                this.initializeDropdown();
            }
        },
        turnOnEditing: function(){
            this.model.set('isEditing', true);
        },
        turnOffEditing: function(){
            this.model.set('isEditing', false);
        },
        onDestroy: function(){
            //ensure that if a dropdown goes away, it's companion does too
            if (!this.dropdownCompanion.isDestroyed) {
                this.dropdownCompanion.destroy();
            }
        },
        isCentered: true,
        getCenteringElement: function(){
            return this.el.querySelector('.dropdown-text');
        },
        serializeData: function(){
            if (this.options.list){
                var values = this.model.get('value');
                var selections = values.map(function(value){
                     var selection = this.options.list.filter(function(item){
                         return item.value.toString() === value.toString();
                     });
                    if (selection.length > 0){
                        return selection[0];
                    } else {
                        return {
                            invalid: true,
                            label: value
                        };
                    }
                }.bind(this));
                return selections;
            } else {
                return this.model.toJSON();
            }
        }
    }, {
        createSimpleDropdown: function(options){
            return new this({
                model: new DropdownModel({
                    value: options.defaultSelection
                }),
                list: options.list,
                hasFiltering: options.hasFiltering,
                componentToShow: SelectView,
                isMultiSelect: options.isMultiSelect,
                defaultSelection: options.defaultSelection,
                customChildView: options.customChildView
            });
        }
    });
});
