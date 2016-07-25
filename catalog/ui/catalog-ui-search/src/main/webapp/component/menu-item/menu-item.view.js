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
    './action/menu-item.action.hbs',
    './component/menu-item.component.hbs',
    './submenu/menu-item.submenu.hbs',
    'js/CustomElements'
], function (Marionette, _, $, ActionTemplate, ComponentTemplate, SubmenuTemplate, CustomElements) {

    return Marionette.ItemView.extend({
        getTemplate: function(){
            switch(this.model.type){
                case 'action':
                    return ActionTemplate;
                    break;
                case 'component':
                    return ComponentTemplate;
                    break;
                case 'submenu':
                    return SubmenuTemplate;
                    break;
            }
        },
        className: function(){
            switch(this.model.type){
                case 'action':
                    return 'is-action';
                    break;
                case 'component':
                    return 'is-component';
                    break;
                case 'submenu':
                    return 'is-submenu';
                    break;
            }
        },
        tagName: CustomElements.register('menu-item'),
        attributes: function(){
            var help = this.model.get('help');
            if (help){
                return {
                    'data-help': help
                };
            }
        },
        events: {
            'click': 'handleClick'
        },
        handleClick: function(){
            this.model.execute();
        },
        serializeData: function(){
            var serializedData = this.model.toJSON();
            var readableShortcut = '';
            if (serializedData.shortcut){
                serializedData.shortcut.specialKeys.forEach(function(key, index){
                    if (index > 0){
                        readableShortcut+='+';
                    }
                    readableShortcut+=key;
                });
                readableShortcut+='+';
                serializedData.shortcut.keys.forEach(function(key,index){
                    if (index > 0) {
                        readableShortcut += '+';
                    }
                    readableShortcut+=key;
                });
            }
            serializedData.readableShortcut = readableShortcut;
            return serializedData;
        }
    });
});
