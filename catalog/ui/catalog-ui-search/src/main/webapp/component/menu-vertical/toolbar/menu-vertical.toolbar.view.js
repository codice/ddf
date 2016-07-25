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
    '../menu-vertical.view',
    '../file/menu-vertical.file',
    '../tools/menu-vertical.tools',
    '../edit/menu-vertical.edit',
    '../viewmenu/menu-vertical.viewmenu',
    '../help/menu-vertical.help.view'
], function (Marionette, _, $, MenuVerticalView, FileMenu, ToolsMenu, EditMenu, ViewMenu, HelpMenu) {

    return MenuVerticalView.extend({
        className: 'is-toolbar'
    }, {
        getNewMenu: function(linkedModel, targetElement, name, collection){
            return new this({
                collection: collection,
                linkedModel: linkedModel,
                getTargetElement: targetElement,
                name: name
            });
        },
        getNewFileMenu: function (linkedModel, targetElement, name) {
            return this.getNewMenu(linkedModel, targetElement, name, FileMenu.getNew());
        },
        getNewToolsMenu: function (linkedModel, targetElement, name) {
            return this.getNewMenu(linkedModel, targetElement, name, ToolsMenu.getNew());
        },
        getNewEditMenu: function(linkedModel, targetElement, name){
            return this.getNewMenu(linkedModel, targetElement, name, EditMenu.getNew());
        },
        getNewViewMenu: function(linkedModel, targetElement, name){
            return this.getNewMenu(linkedModel, targetElement, name, ViewMenu.getNew());
        },
        getNewHelpMenu: function(linkedModel, targetElement, name){
            return this.getNewMenu(linkedModel, targetElement, name, HelpMenu.getNew());
        }
    });
});
