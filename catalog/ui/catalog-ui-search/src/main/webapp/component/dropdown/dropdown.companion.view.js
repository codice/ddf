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
    'text!./dropdown.companion.hbs'
], function (Marionette, _, $, CustomElements, template) {

    function hasBottomRoom(top, element){
        return ((top + element.clientHeight) < window.innerHeight);
    }

    function hasRightRoom(left, element){
        return ((left + element.clientWidth) < window.innerWidth);
    }

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('dropdown-companion'),
        regions: {
            componentToShow: '.dropdown-companion-component'
        },
        events: {
        },
        initialize: function(){
            $('body').append(this.el);
            this.render();
            this.handleTail();
            this.componentToShow.show(new this.options.linkedView.componentToShow({
                model: this.options.linkedView.modelForComponent
            }));
            this.listenTo(this.options.linkedView.model, 'change:isOpen', this.handleOpenChange);
            this.listenForClose();
        },
        updatePosition: function () {
            if (this.options.linkedView.isCentered){
                var clientRect = this.options.linkedView.getCenteringElement().getBoundingClientRect();
                var menuWidth = this.el.clientWidth;
                var necessaryLeft = Math.floor(clientRect.left + clientRect.width / 2 - menuWidth / 2);
                var necessaryTop = Math.floor(clientRect.top + clientRect.height);
                if (hasBottomRoom(necessaryTop, this.el)){
                    this.$el.addClass('is-bottom').removeClass('is-top');
                    this.$el.css('left', necessaryLeft).css('top', necessaryTop);
                } else {
                    this.$el.addClass('is-top').removeClass('is-bottom');
                    this.$el.css('left', necessaryLeft).css('top', clientRect.top);
                }
                if(!hasRightRoom(necessaryLeft, this.el)){
                    this.$el.css('left', window.innerWidth-menuWidth-2);
                }
            } else {
                var clientRect = this.options.linkedView.el.getBoundingClientRect();
                this.$el.css('left', clientRect.left).css('top', clientRect.top + clientRect.height);
            }
        },
        handleTail: function(){
            this.$el.toggleClass('has-tail', this.options.linkedView.hasTail);
        },
        handleOpenChange: function(){
            var isOpen = this.options.linkedView.model.get('isOpen');
            if (isOpen){
                this.onOpen();
            } else {
                this.onClose();
            }
        },
        onOpen: function () {
            this.updatePosition();
            this.$el.addClass('is-open');
            this.listenForOutsideClick();
            this.listenForResize();
            //this.listenForScroll();
        },
        onClose: function () {
            this.$el.removeClass('is-open');
            this.stopListeningForOutsideClick();
            this.stopListeningForResize();
            //this.stopListeningForScroll();
        },
        close: function(){
            this.options.linkedView.model.close();
        },
        listenForClose: function(){
            this.$el.on('closeDropdown.'+CustomElements.getNamespace(), function(){
                this.close();
            }.bind(this));
        },
        stopListeningForClose: function(){
            this.$el.off('closeDropdown.'+CustomElements.getNamespace());
        },
        listenForOutsideClick: function () {
            $('body').on('mousedown.' + this.cid, function (event) {
                if (this.$el.find(event.target).length === 0) {
                    this.close();
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
        }
    }, {
        getNewCompanionView: function (linkedView) {
            return new this({
                linkedView: linkedView
            });
        }
    });
});
