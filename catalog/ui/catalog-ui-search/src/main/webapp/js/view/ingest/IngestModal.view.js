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
    'underscore',
    'marionette',
    'backbone',
    'jquery',
    'js/view/Modal',
    './IngestUploadList.view',
    'text!templates/ingest/ingestModal.handlebars',
    'cometdinit',
    'wreqr',
    'bootstrapselect'
], function (_, Marionette, Backbone, $, Modal, UploadList, ingestModalTemplate, Cometd, wreqr) {
    var IngestModal = Modal.extend({
        template: ingestModalTemplate,
        events: {
            'click .start-upload': 'startUpload',
            'click .cancel-uploads': 'cancelPendingUploads',
            'click .finish-upload': 'finishUpload'
        },
        initialize: function () {
            // there is no automatic chaining of initialize.
            Modal.prototype.initialize.apply(this, arguments);
            this.model = new Backbone.Model({ state: 'start' });
            this.listenTo(this.model, 'change:state', this.updateModalControls);
            this.collection = new Backbone.Collection();
            this.listenTo(this.collection, 'add', this.checkIfDialogComplete);
            this.listenTo(this.collection, 'remove', this.checkIfDialogComplete);
        },
        regions: { fileUploadListRegion: '.file-upload-region' },
        onRender: function () {
            var view = this;
            view.$('[data-toggle="upload-popover"]').popover();
            view.fileUploadListRegion.show(new UploadList({ collection: view.collection }));
            this.$('.fileupload').fileupload({
                url: '/services/content',
                paramName: 'file',
                dataType: 'json',
                formData: { directive: 'STORE_AND_PROCESS' },
                maxFileSize: 5000000,
                add: function (e, data) {
                    // this overrides the add to use our own model to control when the upload actually happens.
                    var that = this;
                    var model = view.buildModelFromFileData(data);
                    // we need to pass the parameters along to the model so it knows what to do when it submits.
                    model.fileuploadObject = {
                        ref: that,
                        e: e,
                        data: data
                    };
                    view.collection.add(model);
                },
                done: function (e, data) {
                    var attrs = {};
                    attrs.name = data.files[0].name;
                    var model = view.collection.findWhere(attrs);
                    model.set('state', 'done');
                    view.checkIfDialogComplete();
                },
                fail: function (e, data) {
                    var attrs = {};
                    attrs.name = data.files[0].name;
                    var model = view.collection.findWhere(attrs);
                    if (data.textStatus === 'abort') {
                        model.set('state', 'cancelled');
                    } else {
                        model.set({
                            state: 'failed',
                            error: data.errorThrown
                        });
                    }
                    view.checkIfDialogComplete();
                },
                progress: function (e, data) {
                    var progress = parseInt(data.loaded / data.total * 100, 10);
                    var attrs = {};
                    attrs.name = data.files[0].name;
                    var model = view.collection.findWhere(attrs);
                    if (model) {
                        model.set('progress', progress);
                    }
                }
            });
            view.$('.fileupload').fileupload('option', { dropZone: view.$el });
        },
        onBeforeClose: function () {
            this.$('.fileupload').fileupload('destroy');
        },
        buildModelFromFileData: function (data) {
            var name = data.files[0].name.toLowerCase();
            var type = data.files[0].type;
            if (type === '') {
                if (name.lastIndexOf('kar') === name.length - 3) {
                    type = 'KAR';
                } else if (name.lastIndexOf('jar') === name.length - 3) {
                    type = 'JAR';
                }
            }
            return new Backbone.Model({
                name: data.files[0].name,
                size: data.files[0].size,
                type: type,
                state: 'start',
                error: data.errorThrown,
                cancellable: true,
                progress: parseInt(data.loaded / data.total * 100, 10)
            });
        },
        checkIfDialogComplete: function () {
            var complete = true;
            var succeeded = 0, failed = 0, cancelled = 0;
            this.collection.each(function (item) {
                switch (item.get('state')) {
                case 'done':
                    ++succeeded;
                    break;
                case 'failed':
                    ++failed;
                    break;
                case 'cancelled':
                    ++cancelled;
                    break;
                case 'uploading':
                    complete = false;
                    break;
                }
            });
            if (this.collection.length > 0) {
                if (complete && this.model.get('state') === 'uploading') {
                    this.model.set('state', 'uploaded');
                    this.sendNotification(succeeded, failed, cancelled);
                }
                this.disableStartUploadButton(false);
            } else {
                this.disableStartUploadButton(true);
            }
        },
        sendNotification: function (succeeded, failed, cancelled) {
            var notification = {
                application: 'Uploads',
                title: 'File Upload Complete',
                message: succeeded + ' succeeded, ' + failed + ' failed, ' + cancelled + ' cancelled',
                timestamp: Date.now()
            };
            Cometd.Comet.publish('/ddf/notifications', notification);
        },
        startUpload: function () {
            this.model.set('state', 'uploading');
            this.collection.trigger('startUpload');
            wreqr.vent.trigger('upload:start');
        },
        finishUpload: function () {
            this.model.set('state', 'finished');
            wreqr.vent.trigger('upload:finish');
        },
        cancelPendingUploads: function () {
            this.collection.each(function (uploadItem) {
                uploadItem.fileuploadObject.data.abort();
            });
        },
        disableStartUploadButton: function (disable) {
            this.$('.start-upload').prop('disabled', disable);
        },
        updateModalControls: function () {
            if (this.model.get('state') === 'uploading') {
                this.$('.uploadFields').toggleClass('hidden', true);
                this.$('.cancelFields').toggleClass('hidden', false);
                this.$('.fileinput-button').prop('disabled', true);
                this.$('.fileupload').fileupload('disable');
                this.$('.drop-zone').hide();
                this.$('.upload-info').toggleClass('hidden', false);
            } else if (this.model.get('state') === 'uploaded') {
                this.$('.cancelFields').toggleClass('hidden', true);
                this.$('.finishFields').toggleClass('hidden', false);
                this.$('.upload-info').toggleClass('hidden', true);
            }
        },
        isUnfinished: function () {
            return this.model.get('state') === 'uploading' || this.model.get('state') === 'uploaded';
        }
    });
    return IngestModal;
});
