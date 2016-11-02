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
    'component/loading/loading.view',
    'component/lightbox/lightbox.view.instance',
    'component/workspace-sharing/workspace-sharing.view',
    'component/metacard-restore/metacard-restore.view'
], function (_, Backbone, Vertical, store, wreqr, LoadingView, lightboxInstance, WorkspaceSharing, MetacardRestore) {

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
