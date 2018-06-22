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
/*global require*/
var wreqr = require('wreqr');
var Marionette = require('marionette');
var _ = require('underscore');
var $ = require('jquery');
var template = require('./ingest.hbs');
var CustomElements = require('js/CustomElements');
var router = require('component/router/router');
var IngestDetails = require('component/ingest-details/ingest-details.view');
var IngestEditor = require('component/ingest-editor/ingest-editor.view');
var properties = require('properties');
var announcement = require('component/announcement');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('ingest'),
    modelEvents: {},
    events: {},
    ui: {},
    regions: {
        ingestDetails: '.ingest-details',
        ingestEditor: '.ingest-editor',
    },
    detailsView: {},
    editorView: {},
    childEvents: {
        'ingestDetails:on:upload': 'preIngestValidation',
    },
    initialize: function() {
        this.listenTo(router, 'change', this.handleRoute);
    },
    handleRoute: function() {
        if (router.toJSON().name === 'openIngest' && !properties.isUploadEnabled()) {
            router.notFound();
        }
    },
    onRender() {
        this.handleRoute();
    },
    onBeforeShow: function() {
        this.editorView = new IngestEditor();
        this.ingestEditor.show(this.editorView);
        this.detailsView = new IngestDetails({url: '/services/catalog/'});
        this.ingestDetails.show(this.detailsView);

        if (properties.editorAttributes.length === 0) {
            this.ingestEditor.$el.hide();
        }
    },
    /* todo: Check for validation issues before uploading */
    preIngestValidation: function () {
        var propertyCollectionView = this.editorView.getPropertyCollectionView();
        propertyCollectionView.updateRequiredPropertyHighlighting();
        if (propertyCollectionView.hasBlankRequiredAttributes()) {
            announcement.announce({
                title: 'Upload not Permitted',
                message: 'Please make sure all required attributes are set before uploading',
                type: 'error'
            });
        } else {
            this.detailsView.setOverrides(this.editorView.getAttributeOverrides());
            this.detailsView.startUpload();
        }
    }
});
