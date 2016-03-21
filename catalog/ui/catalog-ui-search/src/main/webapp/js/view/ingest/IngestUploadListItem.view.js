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
    'jquery',
    'text!templates/ingest/ingestNewFileRow.handlebars',
    'fileupload'
], function (Marionette, $, ingestNewFileRow) {
    var UploadItem = Marionette.ItemView.extend({
        template: ingestNewFileRow,
        tagName: 'li',
        className: 'file-list-item',
        events: {
            'click .start-upload': 'startUpload',
            'click .remove-upload': 'removeUpload'
        },
        modelEvents: {
            'change:progress': 'changeProgress',
            'change:state': 'handleStateChange',
            // only re-render on state changes,
            'startItemUpload': 'startUpload'
        },
        handleStateChange: function () {
            var cancellable = this.model.get('state') === 'start' || this.model.get('state') === 'uploading';
            this.model.set('cancellable', cancellable);
            this.trigger('stateChanged');
            this.render();
        },
        startUpload: function () {
            if (this.model.get('state') === 'start') {
                this.model.set({ 'state': 'uploading' });
                $.blueimp.fileupload.prototype.options.add.call(this.model.fileuploadObject.ref, this.model.fileuploadObject.e, this.model.fileuploadObject.data);
                this.model.trigger('uploading');    // push up to whoever wants to listen.
            }
        },
        doneUpload: function () {
            this.model.set({ 'state': 'done' });
        },
        changeProgress: function () {
            var progress = this.model.get('progress');
            this.$('.progress-percent-text').html(progress + '%');
            this.$('.progress-bar').attr('aria-valuenow', progress).css({ width: progress + '%' });
        },
        removeUpload: function () {
            if (this.model.get('state') === 'uploading') {
                this.model.fileuploadObject.data.abort();
            } else if (this.model.get('state') === 'start') {
                this.model.trigger('removeUpload', this);
            }
        }
    });
    return UploadItem;
});
