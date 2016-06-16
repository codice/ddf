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
                name: 'Save',
                icon: 'floppy-o',
                shortcut: {
                    specialKeys: [
                        'Ctrl'
                    ],
                    keys: [
                        'S'
                    ]
                },
                action: function(){
                    store.saveCurrentWorkspace();
                }
            },
            {
                type: 'action',
                name: 'Open Workspace',
                icon: 'folder-o',
                shortcut: {
                    specialKeys: [
                        'Ctrl'
                    ],
                    keys: [
                        'O'
                    ]
                },
                action: function(){
                    wreqr.vent.trigger('router:navigate', {
                        fragment: 'workspaces',
                        options: {
                            trigger: true
                        }
                    });
                }
            },
            {
                type: 'action',
                name: 'Move to Trash',
                icon: 'trash-o',
                shortcut: {
                    specialKeys: [
                        'Ctrl'
                    ],
                    keys: [
                        'D'
                    ]
                },
                action: function(){
                    var loadingview = new LoadingView();
                    store.getCurrentWorkspace().once('sync', function(){
                        wreqr.vent.trigger('router:navigate', {
                            fragment: 'workspaces',
                            options: {
                                trigger: true
                            }
                        });
                        loadingview.remove();
                    });
                    store.getCurrentWorkspace().destroy({
                        wait: true
                    });
                }
            },
            {
                type: 'action',
                name: 'Run All Queries',
                icon: 'play',
                action: function(){
                    store.getCurrentQueries().forEach(function(query){
                        query.startSearch();
                    });
                }
            },
            {
                type: 'action',
                name: 'Cancel All Queries',
                icon: 'stop',
                action: function(){
                    store.getCurrentQueries().forEach(function(query){
                        query.cancelCurrentSearches();
                    });
                }
            }
        ],
        [
            {
                type: 'action',
                name: 'See Revision History',
                icon: 'history'
            }
        ],
        [
            {
                type: 'action',
                name: 'Share',
                icon: 'users'
            }
        ]
    ];

    return Vertical.extend({
    }, {
        getNew: function(){
            return new this(definition);
        }
    });
});
