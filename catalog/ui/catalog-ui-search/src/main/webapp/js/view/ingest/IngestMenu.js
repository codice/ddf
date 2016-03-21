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
    'text!templates/ingest/ingest.menuItem.handlebars',
    'js/view/ingest/IngestModal.view',
    'wreqr'
], function (Marionette, ingestMenuItem, IngestModal, wreqr) {
    var IngestMenu = Marionette.LayoutView.extend({
        tagName: 'li',
        modelEvents: { 'change': 'render' },
        className: 'dropdown',
        template: ingestMenuItem,
        regions: { modalRegion: '.modal-region' },
        events: { 'click .showModal': 'showModal' },
        initialize: function () {
            this.listenTo(wreqr.vent, 'upload:start', this.onUploadStart);
            this.listenTo(wreqr.vent, 'upload:finish', this.onUploadFinish);
        },
        showModal: function () {
            var keepCurrentModal = this.modal && this.modal.isUnfinished();
            if (!keepCurrentModal) {
                this.modal = new IngestModal();
            }
            wreqr.vent.trigger('showModal', this.modal);
        },
        onUploadStart: function () {
            this.model.set('uploading', true);
        },
        onUploadFinish: function () {
            this.model.set('uploading', false);
        }
    });
    return IngestMenu;
});
