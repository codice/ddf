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
    'component/lightbox/lightbox.view.instance',
    'js/view/preferences/PreferencesModal.view',
    'component/alert-settings/alert-settings.view',
    'component/singletons/user-instance'
], function (_, Backbone, Vertical, lightboxInstance, PreferencesModalView, AlertSettingsView, user) {

    var definition = [
        [
            {
                type: 'action',
                name: 'Map Preferences',
                icon: 'cog',
                shortcut: {
                    specialKeys: [
                        'Ctrl'
                    ],
                    keys: [
                        'M'
                    ]
                },
                action: function () {
                    lightboxInstance.model.updateTitle('Map Preferences');
                    lightboxInstance.model.open();
                    lightboxInstance.lightboxContent.show(new PreferencesModalView());
                }
            },
            {
                type: 'action',
                name: 'Alert Preferences',
                icon: 'bell',
                action: function () {
                    lightboxInstance.model.updateTitle('Alert Preferences');
                    lightboxInstance.model.open();
                    lightboxInstance.lightboxContent.show(new AlertSettingsView());
                }
            }
        ],
        [
            {
                type: 'action',
                name: 'Map',
                icon: 'globe',
                help: 'Switches the visualization to a map.',
                action: function () {
                    user.get('user').get('preferences').set('visualization', 'map');
                }
            },
            {
                type: 'action',
                name: 'Histogram',
                icon: 'bar-chart',
                help: 'Switches the visualization to a histogram.',
                action: function () {
                    user.get('user').get('preferences').set('visualization', 'histogram');
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
