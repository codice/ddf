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
    'component/ingest/ingest.view',
    'js/view/ingest/IngestModal.view'
], function (_, Backbone, Vertical, lightboxInstance, IngestView, IngestModalView) {

    var definition = [
        [
            {
                type: 'action',
                name: 'Ingest',
                icon: 'upload',
                shortcut: {
                    specialKeys: [
                        'Ctrl'
                    ],
                    keys: [
                        'I'
                    ]
                },
                action: function () {
                    lightboxInstance.model.updateTitle('Ingest');
                    lightboxInstance.model.open();
                    lightboxInstance.lightboxContent.show(new IngestModalView());
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
