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
/*global require, window, setTimeout*/
var wreqr = require('wreqr');
var Marionette = require('marionette');
var _ = require('underscore');
var $ = require('jquery');
var template = require('./ingest-details.hbs');
var CustomElements = require('js/CustomElements');
var Dropzone = require('dropzone');
var UploadItemCollectionView = require('component/upload-item/upload-item.collection.view');
var UploadBatchModel = require('js/model/UploadBatch');
var userInstance = require('component/singletons/user-instance');
var Common = require('js/Common');
var UploadSummary = require('component/upload-summary/upload-summary.view');

function namespacedEvent(event, view) {
    return event + '.' + view.cid;
}

function updateDropzoneHeight(view) {
    var filesHeight = view.$el.find('.details-files').height();
    var elementHeight = view.$el.height();
    view.$el.find('.details-dropzone').css('height', 'calc('+elementHeight+'px - '+filesHeight+'px - 20px - 2.75rem' + ')');
}

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('ingest-details'),
    events: {
        'click > .details-footer .footer-clear': 'newUpload',
        'click > .details-footer .footer-start': 'startUpload',
        'click > .details-footer .footer-cancel': 'cancelUpload',
        'click > .details-footer .footer-new': 'newUpload',
        'click > .details-dropzone .dropzone-text': 'addFiles'
    },
    regions: {
        files: '> .details-files',
        summary: '> .details-summary'
    },
    dropzone: undefined,
    uploadBatchModel: undefined,
    dropzoneAnimationRequestDetails: undefined,
    triggerNewUpload: function() {
        this.onBeforeDestroy();
        this.render();
        this.onBeforeShow();
    },
    onBeforeShow: function() {
        this.setupDropzone();
        this.setupBatchModel();
        this.showFiles();
        this.showSummary();
        this.$el.removeClass();
        this.handleUploadUpdate();
    },
    setupBatchModel: function() {
        this.uploadBatchModel = new UploadBatchModel({}, {
            dropzone: this.dropzone
        });
        this.setupBatchModelListeners();
    },
    setupBatchModelListeners: function() {
        this.listenTo(this.uploadBatchModel, 'add:uploads remove:uploads reset:uploads', this.handleUploadUpdate);
        this.listenTo(this.uploadBatchModel, 'change:sending', this.handleSending);
        this.listenTo(this.uploadBatchModel, 'change:finished', this.handleFinished);
    },
    handleFinished: function() {
        this.$el.toggleClass('is-finished', this.uploadBatchModel.get('finished'));
    },
    handleSending: function() {
        this.$el.toggleClass('is-sending', this.uploadBatchModel.get('sending'));
    },
    handleUploadUpdate: function() {
        if (this.uploadBatchModel.get('uploads').length === 0 && !this.uploadBatchModel.get('sending')) {
            Common.cancelRepaintForTimeframe(this.dropzoneAnimationRequestDetails);
            this.$el.toggleClass('has-files', false);
            this.unlistenToResize();
            this.$el.find('.details-dropzone').css('height', '');
        } else {
            this.$el.toggleClass('has-files', true);
            this.updateDropzoneHeight();
        }
    },
    setupDropzone: function() {
        this.dropzone = new Dropzone(this.el.querySelector('.details-dropzone'), {
            url: this.options.url,
            maxFilesize: 5000000, //MB
            method: 'post',
            autoProcessQueue: false,
            headers: this.options.extraHeaders
        });
        if (this.options.handleUploadSuccess) {
            this.dropzone.on('success', this.options.handleUploadSuccess);
        }
    },
    addFiles: function(){
        this.$el.find('.details-dropzone').click();
    },
    showFiles: function() {
        this.files.show(new UploadItemCollectionView({
            collection: this.uploadBatchModel.get('uploads')
        }));
    },
    showSummary: function() {
        this.summary.show(new UploadSummary({
            model: this.uploadBatchModel
        }));
    },
    clearUploads: function() {
        this.uploadBatchModel.clear();
    },
    startUpload: function() {
        this.uploadBatchModel.start();
    },
    cancelUpload: function() {
        this.uploadBatchModel.cancel();
    },
    newUpload: function() {
        this.$el.addClass('starting-new');
        setTimeout(function() {
            this.triggerNewUpload();
        }.bind(this), 250);
    },
    expandUpload: function() {
        wreqr.vent.trigger('router:navigate', {
            fragment: 'uploads/' + this.uploadBatchModel.id,
            options: {
                trigger: true
            }
        });
    },
    updateDropzoneHeight: function() {
        updateDropzoneHeight(this);
        this.listenToResize();
        Common.cancelRepaintForTimeframe(this.dropzoneAnimationRequestDetails);
        this.dropzoneAnimationRequestDetails = Common.repaintForTimeframe(2000, updateDropzoneHeight.bind(this, this));
    },
    listenToResize: function() {
        $(window).off(namespacedEvent('resize', this)).on(namespacedEvent('resize', this), this.updateDropzoneHeight.bind(this));
    },
    unlistenToResize: function() {
        $(window).off(namespacedEvent('resize', this));
    },
    onBeforeDestroy: function() {
        this.stopListening(this.uploadBatchModel);
        this.unlistenToResize();
    },
});