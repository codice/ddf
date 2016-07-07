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

    function hasLeftRoom(left){
        return left > 0;
    }

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('dropdown-companion'),
        regions: {
            componentToShow: '.dropdown-companion-component'
        },
        events: {
            'keydown': 'handleSpecialKeys',
            'keyup .dropdown-companion-filter': 'handleFilterUpdate',
            'mousedown': 'handleMousedown'
        },
        attributes: {
            'tabindex': 0
        },
        initialize: function(){
            this.listenTo(this.options.linkedView.model, 'change:isOpen', this.handleOpenChange);
            this.listenForClose();
        },
        updateWidth: function(){
           // if (this.options.linkedView.matchWidth){
                var clientRect = this.options.linkedView.getCenteringElement().getBoundingClientRect();
                this.$el.css('min-width', clientRect.width);
           // }
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
                if(!hasLeftRoom(necessaryLeft)){
                    this.$el.css('left', 10);
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
            if (!this.el.parentElement){
                this.render();
                this.handleTail();
                var componentToShow = this.options.linkedView.componentToShow ||  this.options.linkedView.options.componentToShow;
                this.componentToShow.show(new componentToShow(_.extend(this.options.linkedView.options,{
                    model: this.options.linkedView.modelForComponent
                })));
                $('body').append(this.el);
            }
            this.updateWidth();
            this.updatePosition();
            this.$el.addClass('is-open');
            this.listenForOutsideClick();
            this.listenForResize();
            this.focusOnFilter();
            this.handleFiltering();
            //this.listenForScroll();
        },
        focusOnFilter: function(){
            var hasFiltering = Boolean(this.options.linkedView.hasFiltering || this.options.linkedView.options.hasFiltering);
            if (hasFiltering) {
                setTimeout(function () {
                    this.$el.children('input').focus()
                }.bind(this), 0);
            } else {
                this.$el.focus();
            }
        },
        handleFiltering: function(){
            var hasFiltering = Boolean(this.options.linkedView.hasFiltering || this.options.linkedView.options.hasFiltering);
            this.$el.toggleClass('has-filtering', hasFiltering);
        },
        handleFilterUpdate: function(event){
            var code = event.keyCode;
            if (event.charCode && code == 0)
                code = event.charCode;
            switch(code) {
                case 13:
                    // Enter
                case 27:
                    // Escape
                case 37:
                    // Key left.
                case 39:
                    // Key right.
                case 38:
                    // Key up.
                case 40:
                    // Key down
                    break;
                default:
                    var filterValue = this.$el.children('input').val();
                    this.componentToShow.currentView.handleFilterUpdate(filterValue);
                    break;
            }
        },
        handleSpecialKeys: function(event){
            var code = event.keyCode;
            if (event.charCode && code == 0)
                code = event.charCode;
            switch(code) {
                case 13:
                    // Enter
                    event.preventDefault();
                    if (this.componentToShow.currentView.handleEnter)
                        this.componentToShow.currentView.handleEnter();
                    break;
                case 27:
                    // Escape
                    event.preventDefault();
                    this.handleEscape();
                    break;
                case 38:
                    // Key up.
                    event.preventDefault();
                    if (this.componentToShow.currentView.handleUpArrow)
                        this.componentToShow.currentView.handleUpArrow();
                    break;
                case 40:
                    // Key down.
                    event.preventDefault();
                    if (this.componentToShow.currentView.handleDownArrow)
                        this.componentToShow.currentView.handleDownArrow();
                    break;
            }
        },
        onClose: function () {
            if (this.el.parentElement) {
                this.$el.removeClass('is-open');
            }
            this.stopListeningForOutsideClick();
            this.stopListeningForResize();
            //this.stopListeningForScroll();
        },
        close: function(){
            this.options.linkedView.model.close();
        },
        handleEscape: function(){
            this.close();
        },
        listenForClose: function(){
            this.$el.on('closeDropdown.'+CustomElements.getNamespace(), function(e){
                // stop from closing dropdowns higher in the dom
                e.stopPropagation();
                // close
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
        },
        onDestroy: function(){
            this.stopListeningForClose();
            this.stopListeningForOutsideClick();
            this.stopListeningForResize;
        },
        handleMousedown: function(e){
            // stop from closing dropdowns higher in the dom
            e.stopPropagation();
        }
    }, {
        getNewCompanionView: function (linkedView) {
            return new this({
                linkedView: linkedView
            });
        }
    });
});
