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
    'component/ingest/ingest.view'
], function (_, Backbone, Vertical, lightboxInstance, IngestView) {

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
                    lightboxInstance.model.updateTitle('Recent Uploads');
                    lightboxInstance.model.open();
                    lightboxInstance.lightboxContent.show(new IngestView());
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
