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
    'component/menu-item/menu-item.collection.view',
    'js/CustomElements'
], function (Marionette, _, $, MenuItemCollectionView, CustomElements) {

    return Marionette.CollectionView.extend({
        childView: MenuItemCollectionView,
        tagName: CustomElements.register('menu-vertical'),
        events: {
            'click ddf-menu-item.is-action': 'handleClick'
        },
        initialize: function () {
            $('body').append(this.el);
            this.render();
            this.listenTo(this.options.linkedModel, 'change:activeMenu', this.handleActiveMenu);
        },
        open: function () {
            this.updatePosition();
            this.$el.addClass('is-open');
            this.listenForOutsideClick();
            this.listenForResize();
            this.listenForScroll();
        },
        close: function () {
            this.$el.removeClass('is-open');
            this.stopListeningForOutsideClick();
            this.stopListeningForResize();
            this.stopListeningForScroll();
        },
        handleClick: function () {
            this.options.linkedModel.close();
        },
        updatePosition: function () {
            var clientRect = this.options.getTargetElement().getBoundingClientRect();
            this.$el.css('left', clientRect.left).css('top', clientRect.top + clientRect.height);
        },
        listenForOutsideClick: function () {
            $('body').on('mousedown.' + this.cid, function (event) {
                if (this.$el.find(event.target).length === 0) {
                    this.handleClick();
                }
            }.bind(this));
        },
        stopListeningForOutsideClick: function () {
            $('body').off('mousedown.' + this.cid);
        },
        listenForResize: function(){
            $(window).on('resize.'+this.cid, _.throttle(function(event){
                this.updatePosition();
            }.bind(this), 16));
        },
        stopListeningForResize: function(){
            $(window).off('resize.'+this.cid);
        },
        listenForScroll: function(){
            $('*').on('scroll.'+this.cid, _.throttle(function(event){
                this.updatePosition();
            }.bind(this), 16));
        },
        stopListeningForScroll: function(){
            $('*').off('scroll.'+this.cid);
        },
        handleActiveMenu: function () {
            if (this.options.linkedModel.getActiveMenu() === this.options.name) {
                this.open();
            } else {
                this.close();
            }
        }
    });
});
