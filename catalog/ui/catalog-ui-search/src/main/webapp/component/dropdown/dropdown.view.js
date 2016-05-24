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
    './dropdown.companion.view'
], function (Marionette, _, $, CustomElements, DropdownCompanionView) {

    return Marionette.ItemView.extend({
        tagName: CustomElements.register('dropdown'),
        events: {
            'click': 'handleClick'
        },
        handleClick: function(){
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
            this.modelForComponent = new this.modelForComponent();
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
            this.dropdownCompanion.updatePosition();
        },
        turnOnEditing: function(){
            this.model.set('isEditing', true);
        },
        turnOffEditing: function(){
            this.model.set('isEditing', false);
        }
    });
});
