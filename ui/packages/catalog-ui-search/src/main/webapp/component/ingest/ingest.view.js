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
        var isEditorShown = (properties.editorAttributes.length > 0);
        this.$el.toggleClass('editor-hidden', !isEditorShown);
        if (isEditorShown) {
            this.ingestEditor.show(new IngestEditor());
        }
        this.ingestDetails.show(new IngestDetails({
            url: this.options.url || './internal/catalog/',
            extraHeaders: this.options.extraHeaders,
            handleUploadSuccess: this.options.handleUploadSuccess,
            preIngestValidator: isEditorShown ? this.validateAttributes.bind(this) : null
        }));
    },
    validateAttributes: function () {
        var propertyCollectionView = this.ingestEditor.currentView.getPropertyCollectionView();
        if (propertyCollectionView.hasBlankRequiredAttributes() || !propertyCollectionView.isValid()) {
            announcement.announce({
                title: 'Some fields need attention',
                message: 'Please address validation issues before uploading',
                type: 'error'
            });
            propertyCollectionView.showRequiredWarnings();
            return false;
        } else {
            this.ingestDetails.currentView.setOverrides(this.ingestEditor.currentView.getAttributeOverrides());
            propertyCollectionView.hideRequiredWarnings();
            return true;
        }
    }
});
