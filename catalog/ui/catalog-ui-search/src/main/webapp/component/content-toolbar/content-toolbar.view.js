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
    './content-toolbar.hbs',
    'js/CustomElements',
    'component/menu-vertical/toolbar/menu-vertical.toolbar.view',
    './content-toolbar'
], function (Marionette, _, $, template, CustomElements, MenuView, ContentToolbar) {

    return Marionette.ItemView.extend({
        setDefaultModel: function(){
            this.model = new ContentToolbar();
        },
        template: template,
        tagName: CustomElements.register('content-toolbar'),
        events: {
            'click .toolbar-menu': 'clickMenu',
            'mouseover .toolbar-menu': 'hoverMenu'
        },
        initialize: function (options) {
            if (!options.model){
                this.setDefaultModel();
            }
            this.listenTo(this.model, 'change:activeMenu', this.handleActiveMenu);
        },
        initializeMenus: function(){
            this._fileMenu = MenuView.getNewFileMenu(this.model, function () {
                    return this.el.querySelector('.menu-file');
                }.bind(this),
                'file');
            this._editMenu = MenuView.getNewEditMenu(this.model,
                function () {
                    return this.el.querySelector('.menu-edit');
                }.bind(this),
                'edit');
            this._toolsMenu = MenuView.getNewToolsMenu(this.model,
                function () {
                    return this.el.querySelector('.menu-tools');
                }.bind(this),
                'tools');
            this._viewMenu = MenuView.getNewViewMenu(this.model,
                function () {
                    return this.el.querySelector('.menu-view');
                }.bind(this),
                'view');
            this._helpMenu = MenuView.getNewHelpMenu(this.model,
                function () {
                    return this.el.querySelector('.menu-help');
                }.bind(this),
                'help');
        },
        firstRender: true,
        onRender: function(){
            if (this.firstRender){
                this.firstRender = false;
                this.initializeMenus();
            }
        },
        activateMenu: function(menu){
            this.model.activate(menu);
        },
        clickMenu: function(event){
            var menu = $(event.currentTarget).attr('data-menu');
            if (this.model.isOpen()){
                this.model.close();
            } else {
                event.stopPropagation();
                this.activateMenu(menu);
            }
        },
        hoverMenu: function(event){
            var menu = $(event.currentTarget).attr('data-menu');
            if (this.model.isOpen()){
                this.activateMenu(menu);
            }
        },
        handleActiveMenu: function(){
            var activeMenu = this.model.getActiveMenu();
            this.$el.find('.toolbar-menu').removeClass('is-open');
            this.$el.find('.menu-'+activeMenu).addClass('is-open');
        }
    });
});
