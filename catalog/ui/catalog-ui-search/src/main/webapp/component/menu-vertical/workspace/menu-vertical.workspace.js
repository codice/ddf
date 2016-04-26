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
    'underscore',
    'backbone',
    '../menu-vertical',
    'js/store',
    'wreqr',
    'component/loading/loading.view'
], function (_, Backbone, Vertical, store, wreqr, LoadingView) {

    var definition = [
        [
            {
                type: 'action',
                name: 'Rename',
                icon: 'font',
                action: function () {
                    //store.saveCurrentWorkspace();
                }
            },
            {
                type: 'action',
                name: 'Move to trash',
                icon: 'trash-o',
                action: function () {
                    var loadingview = new LoadingView();
                    store.getWorkspaceById(this.id).once('sync', function () {
                        wreqr.vent.trigger('router:navigate', {
                            fragment: 'workspaces',
                            options: {
                                trigger: true
                            }
                        });
                        loadingview.remove();
                    });
                    store.getWorkspaceById(this.id).destroy({
                        wait: true
                    });
                }
            },
            {
                type: 'action',
                name: 'Open in New Tab',
                icon: 'external-link',
                action: function () {
                    var hasSlash = Boolean(location.href.charAt(location.href.length - 1) === '/');
                    window.open(location.href + (hasSlash ? '' : '/') + this.id);
                }
            },
            {
                type: 'action',
                name: 'Sharing',
                icon: 'users',
                action: function () {

                }
            }
        ]
    ];

    return Vertical.extend({}, {
        getNew: function (workspaceModel) {
            var copyDefinition = JSON.parse(JSON.stringify(definition));
            copyDefinition.forEach(function (menuItemGroup, menuItemGroupIndex) {
                menuItemGroup.forEach(function (menuItem, menuItemIndex) {
                    menuItem.action = definition[menuItemGroupIndex][menuItemIndex].action.bind(workspaceModel);
                });
            });
            return new this(copyDefinition);
        }
    });
});
