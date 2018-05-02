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
/* global require*/
const _ = require('underscore');
const Marionette = require('marionette');
const template = require('./layer-item.hbs');
const CustomElements = require('js/CustomElements');

module.exports = Marionette.ItemView.extend({
    template: template,
    tagName: CustomElements.register('layer-item'),
    className: 'group',
    attributes: function(){
        return {
            'data-id': this.model.id
        };
    },
    events: {
        'click .layer-show': 'updateLayerShow',
        'change .layer-alpha': 'updateLayerAlpha',
        'click .layer-rearrange-down': 'moveDown',
        'click .layer-rearrange-up': 'moveUp'
    },
    initialize: function () {
        this.listenTo(this.model, 'change:show', this.handleShow);
        this.listenTo(this.model, 'change:alpha', this.handleAlpha);
        this.listenTo(this.model, 'change:order', this.handleOrder);
    },
    moveDown: function(e) {
        const order = this.options.sortable.toArray();
        const currentIndex = order.indexOf(this.model.id);
        order.splice(currentIndex, 1);
        order.splice(currentIndex + 1, 0, this.model.id);
        this.options.sortable.sort(order);
        this.options.focusModel.setDown(this.model.id);
        this.options.updateOrdering();
    },
    moveUp: function(e) {
        const order = this.options.sortable.toArray();
        const currentIndex = order.indexOf(this.model.id);
        order.splice(currentIndex - 1, 0, this.model.id);
        order.splice(currentIndex + 1, 1);
        this.options.sortable.sort(order);
        this.options.focusModel.setUp(this.model.id);
        this.options.updateOrdering();
    },
    updateLayerShow: function(e){
        this.model.set('show', !this.model.get('show'));
    },
    updateLayerAlpha: function(e) {
        this.model.set('alpha', e.target.value);
    },
    onRender: function () {
        this.handleShow();
        this.handleAlpha();
        this.handleOrder();
    },
    onAttach: function() {
        this.handleLastTouched();
    },
    handleLastTouched: function() {
        if (this.options.focusModel.id === this.model.id) {
            let focusSelector = this.options.focusModel.isUp() ? '.layer-rearrange-up' : '.layer-rearrange-down';
            focusSelector = this.isTop() ? '.layer-rearrange-down' : focusSelector;
            focusSelector = this.isBottom() ? '.layer-rearrange-up' : focusSelector;
            this.$el.find(focusSelector).focus();
        }
    },
    handleAlpha: function() {
        this.$el.find('.layer-alpha').val(this.model.get('alpha'));
    },
    handleShow: function () {
        this.$el.toggleClass('is-show', this.model.get('show'));
        this.$el.find('input').attr('disabled', this.model.get('show') ? null : '');
    },
    isBottom: function() {
        return this.model.collection.last().id === this.model.id;
    },
    isTop: function() {
        return this.model.collection.first().id === this.model.id;
    },
    handleOrder: function() {
        this.$el.toggleClass('is-top', this.isTop());
        this.$el.toggleClass('is-bottom', this.isBottom());
    },
    serializeData: function() {
        return _.defaults(this.model.toJSON(), {
            name: "Untitled"
        });
    }
});