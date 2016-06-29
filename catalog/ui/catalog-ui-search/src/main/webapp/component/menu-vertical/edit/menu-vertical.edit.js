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
    'wreqr',
    'backbone',
    '../menu-vertical',
    'component/lightbox/lightbox.view.instance',
    'component/ingest/ingest.view',
    'js/model/user'
], function (_, wreqr, Backbone, Vertical, lightboxInstance, IngestView, user) {

    var definition = [
        [
            {
                type: 'action',
                name: 'Recent Uploads',
                icon: 'wrench',
                shortcut: {
                    specialKeys: [
                        'Ctrl'
                    ],
                    keys: [
                        'R'
                    ]
                },
                action: function () {
                    wreqr.vent.trigger('router:navigate', {
                        fragment: 'recent',
                        options: {
                            trigger: true
                        }
                    });
                }
            },
            {
                type: 'action',
                name: 'Reset Blacklist',
                icon: 'eye',
                action: function () {
                    var preferences = user.get('user').get('preferences');
                    preferences.resetBlacklist();
                    preferences.savePreferences();
                }
            }
        ]
    ];

    return Vertical.extend({}, {
        getNew: function () {
            return new this(definition);
        }
    });
});
