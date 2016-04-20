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
    'js/CustomElements',
    './file/menu-vertical.file',
    './tools/menu-vertical.tools'
], function (Marionette, _, $, MenuItemCollectionView, CustomElements, FileMenu, ToolsMenu) {

    return Marionette.CollectionView.extend({
        className: 'is-hidden',
        childView: MenuItemCollectionView,
        tagName: CustomElements.register('menu-vertical'),
        events: {
            'click ddf-menu-item.is-action': 'handleClick'
        },
        initialize: function(){
            $('body').append(this.el);
            this.render();
            this.listenTo(this.options.linkedView.model, 'change:activeMenu', this.handleActiveMenu);
        },
        open: function(){
            this.$el.removeClass('is-hidden');
            this.updatePosition();
            this.listenForOutsideClick();
        },
        close: function(){
            this.$el.addClass('is-hidden');
            this.stopListeningForOutsideClick();
        },
        handleClick: function(){
            this.options.linkedView.model.close();
        },
        updatePosition: function(){
            var clientRect = this.options.targetElement.getBoundingClientRect();
            this.$el.css('left', clientRect.left).css('top', clientRect.top + clientRect.height);
        },
        listenForOutsideClick: function(){
            $('body').on('click.'+this.cid,function(event){
                console.log('outside click');
                if (this.$el.find(event.target).length === 0){
                    this.handleClick();
                }
            }.bind(this));
        },
        stopListeningForOutsideClick: function(){
            console.log('detaching listener '+this.cid);
            $('body').off('click.'+this.cid);
        },
        handleActiveMenu: function(){
            if (this.options.linkedView.model.getActiveMenu()===this.options.name){
                this.open();
            } else {
                this.close();
            }
        }
    }, {
        getNewFileMenu: function(linkedView, targetElement, name){
            return new this({
                collection: FileMenu.getNew(),
                linkedView: linkedView,
                targetElement: targetElement,
                name: name
            });
        },
        getNewToolsMenu: function(linkedView, targetElement, name) {
            return new this({
                collection: ToolsMenu.getNew(),
                linkedView: linkedView,
                targetElement: targetElement,
                name: name
            });
        }
    });
});
