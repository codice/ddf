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
    'backbone',
    'marionette',
    'underscore',
    'jquery',
    './select.view',
    'js/CustomElements',
], function (Backbone, Marionette, _, $, childView, CustomElements) {

    return Marionette.CollectionView.extend({
        getChildView: function(){
            return this.options.customChildView || childView;
        },
        tagName: CustomElements.register('select-collection'),
        modelEvents: {
            'change': 'render'
        },
        events: {
            'click .choice': 'handleChoice'
        },
        initialize: function(){
            this.collection = new Backbone.Collection(this.options.list);
        },
        onAddChild: function(childView){
            childView.$el.addClass('choice');
            childView.$el.attr('data-value', JSON.stringify(childView.model.get('value')));
        },
        onRender: function(){
            this.handleValue();
        },
        handleValue: function(){
            var values = this.model.get('value');
            this.$el.children('[data-value]').removeClass('is-selected');
            values.forEach(function(value){
                this.$el.children('[data-value="'+JSON.stringify(value)+'"]').addClass('is-selected');
            }.bind(this));
        },
        handleChoice: function(e){
            if (!this.options.isMultiSelect){
                this.$el.children('.is-selected').removeClass('is-selected');
            }
            $(e.currentTarget).toggleClass('is-selected');
            this.updateValue();
            if (!this.options.isMultiSelect){
                this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
            }
        },
        updateValue: function(){
            var values = _.map(this.$el.children('.is-selected'), function(choice){
                return JSON.parse($(choice).attr('data-value'));
            });
            this.model.set({
                value: values
            });
        }
    });
});
