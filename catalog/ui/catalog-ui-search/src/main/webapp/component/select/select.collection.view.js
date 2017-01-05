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
/*global define, window*/
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
        },
        events: {
            'click .choice': 'handleChoice',
            'mouseenter .choice': 'handleMouseEnter'
        },
        initialize: function(){
            this.collection = new Backbone.Collection(this.options.list);
        },
        onAddChild: function(childView){
            if (!childView.isDestroyed && childView.model) {
                childView.$el.addClass('choice');
                childView.$el.attr('data-value', JSON.stringify(childView.model.get('value')));
                if (childView._index === 0){
                    childView.$el.addClass('is-active');
                }
                this.handleValueForChildView(childView);
            }
        },
        onRender: function(){
            this.handleActive();
        },
        handleValueForChildView: function(childView){
            var values = this.model.get('value');
            values.forEach(function(value){
                    if (childView.$el.attr('data-value') === JSON.stringify(value)) {
                        childView.$el.addClass('is-selected');
                    }
            });
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
                var diff = ($nextActive[0].getBoundingClientRect().top +
                    $nextActive[0].getBoundingClientRect().height) -
                    ($nextActive[0].parentNode.parentNode.clientHeight +
                    $nextActive[0].parentNode.parentNode.getBoundingClientRect().top);
                if (diff >= 0) {
                    $nextActive[0].parentNode.parentNode.scrollTop = $nextActive[0].parentNode.parentNode.scrollTop + diff;
                }
            }
        },
        handleUpArrow: function(){
            var $currentActive = this.$el.children('.choice.is-active');
            var $nextActive = $currentActive.prev();
            if ($nextActive.length !== 0){
                $currentActive.removeClass('is-active');
                $nextActive.addClass('is-active');
                var diff = ($nextActive[0].parentNode.parentNode.getBoundingClientRect().top) -
                    ($nextActive[0].getBoundingClientRect().top);
                if (diff >= 0) {
                    $nextActive[0].parentNode.parentNode.scrollTop = $nextActive[0].parentNode.parentNode.scrollTop - diff;
                }
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
