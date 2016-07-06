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
        emptyView: Marionette.ItemView.extend({className: 'select-collection-empty', template: 'Nothing Found'}),
        getChildView: function(){
            return this.options.customChildView || childView;
        },
        tagName: CustomElements.register('select-collection'),
        modelEvents: {
            'change': 'render'
        },
        events: {
            'click .choice': 'handleChoice',
            'mouseenter .choice': 'handleMouseEnter'
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
            this.handleActive();
        },
        handleValue: function(){
            var values = this.model.get('value');
            var choices = this.$el.children('[data-value]');
            choices.removeClass('is-selected');
            values.forEach(function(value){
                _.forEach(choices, function(choice){
                   if ($(choice).attr('data-value') === JSON.stringify(value)) {
                       $(choice).addClass('is-selected');
                   }
                });
            }.bind(this));
        },
        handleActive: function(){
            this.$el.children('.choice').first().addClass('is-active');
        },
        handleMouseEnter: function(e){
            this.$el.children('.is-active').removeClass('is-active');
            $(e.currentTarget).toggleClass('is-active');
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
        filterValue: '',
        handleFilterUpdate: function(filterValue){
            this.filterValue = filterValue.toLowerCase();
            this.render();
        },
        handleEnter: function(){
            if (!this.options.isMultiSelect){
                this.$el.children('.is-selected').removeClass('is-selected');
            }
            this.$el.children('.choice.is-active').toggleClass('is-selected');
            this.updateValue();
            if (!this.options.isMultiSelect){
                this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
            }
        },
        handleDownArrow: function(){
            var $currentActive = this.$el.children('.choice.is-active');
            var $nextActive = $currentActive.next();
            if ($nextActive.length !== 0){
                $currentActive.removeClass('is-active');
                $nextActive.addClass('is-active');
            }
        },
        handleUpArrow: function(){
            var $currentActive = this.$el.children('.choice.is-active');
            var $nextActive = $currentActive.prev();
            if ($nextActive.length !== 0){
                $currentActive.removeClass('is-active');
                $nextActive.addClass('is-active');
            }
        },
        filter: function(child){
            return child.get('label').toLowerCase().indexOf(this.filterValue) > -1;
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
