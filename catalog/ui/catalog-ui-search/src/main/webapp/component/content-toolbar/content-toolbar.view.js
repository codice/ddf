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
    'text!./content-toolbar.hbs',
    'js/CustomElements',
    'component/menu-vertical/menu-vertical.view',
    './content-toolbar'
], function (Marionette, _, $, template, CustomElements, MenuView, ContentToolbar) {

    return Marionette.ItemView.extend({
        setDefaultModel: function(){
            this.model = new ContentToolbar();
        },
        template: template,
        tagName: CustomElements.register('content-toolbar'),
        events: {
            'click .menu-file': 'clickFile',
            'click .menu-tools': 'clickTools',
            'mouseover .menu-file': 'hoverFile',
            'mouseover .menu-tools': 'hoverTools'
        },
        initialize: function (options) {
            if (!options.model){
                this.setDefaultModel();
            }
            this.listenTo(this.model, 'change:isOpen', this.handleOpen);
            this.listenTo(this.model, 'change:activeMenu', this.handleActiveMenu);
        },
        initializeMenus: function(){
            this._fileMenu = MenuView.getNewFileMenu(this,
                this.el.querySelector('.menu-file'),
                'file');
            this._toolsMenu = MenuView.getNewToolsMenu(this,
                this.el.querySelector('.menu-tools'),
                'tools');
        },
        firstRender: true,
        onRender: function(){
            if (this.firstRender){
                this.firstRender = false;
                this.initializeMenus();
            }
        },
        activateFile: function(){
            this.model.activate('file');
        },
        activateTools: function(){
            this.model.activate('tools');
        },
        clickFile: function(event){
            if (this.model.isOpen()){
                this.model.close();
            } else {
                event.stopPropagation();
                this.activateFile();
            }
        },
        clickTools: function(event){
            if (this.model.isOpen()){
                this.model.close();
            } else {
                event.stopPropagation();
                this.activateTools();
            }
        },
        hoverFile: function(){
            if (this.model.isOpen()){
                this.activateFile();
            }
        },
        hoverTools: function(){
            if (this.model.isOpen()){
                this.activateTools();
            }
        },
        handleOpen: function(){
            console.log(this.model.toJSON());
        },
        handleActiveMenu: function(){
            var activeMenu = this.model.getActiveMenu();
            this.$el.find('.toolbar-menu').removeClass('is-open');
            this.$el.find('.menu-'+activeMenu).addClass('is-open');
        }
    });
});
