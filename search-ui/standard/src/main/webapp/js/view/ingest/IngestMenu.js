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
/* global define */
define([
        'marionette',
        'icanhaz',
        'text!templates/ingest/ingest.menuItem.handlebars',
        'js/view/ingest/IngestModal.view',
        'wreqr'
    ],
    function (Marionette, ich, ingestMenuItem, IngestModal, wreqr) {

        if (!ich.ingestMenuItem) {
            ich.addTemplate('ingestMenuItem', ingestMenuItem);
        }

        var IngestMenu = Marionette.LayoutView.extend({
            tagName: 'li',
            modelEvents: {
                'change': 'render'
            },
            className: 'dropdown',
            template: 'ingestMenuItem',
            regions: {
                modalRegion: '.modal-region'
            },
            events: {
                'click .showModal': 'showModal'
            },
            showModal: function() {
               var modal = new IngestModal({model: this.model});
                wreqr.vent.trigger('showModal', modal);
            }
        });

        return IngestMenu;
    });