/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global require*/
var UploadModel = require('./Upload');
var Backbone = require('backbone');
var Common = require('js/Common');
var wreqr = require('wreqr');
var _ = require('underscore');

var updatePreferences = _.throttle(function(){
    wreqr.vent.trigger('preferences:save');
}, 1000);

function calculatePercentageDone(files){
    if (files.length === 0){
        return 100;
    }
    var totalBytes = files.reduce(function(total, file){
        total += file.upload.total;
        return total;
    }, 0);
    var bytesSent = files.reduce(function(total, file){
        total += file.upload.bytesSent;
        return total;
    }, 0);
    return 100*(bytesSent/totalBytes);
}

module.exports = Backbone.AssociatedModel.extend({
    options: undefined,
    defaults: function() {
        return {
            unseen: true,
            uploads: [],
            percentage: 0,
            errors: 0,
            successes: 0,
            complete: 0,
            amount: 0,
            issues: 0,
            sending: false,
            finished: false,
            interrupted: false,
            sentAt: undefined
        };
    },
    relations: [{
        type: Backbone.Many,
        key: 'uploads',
        relatedModel: UploadModel
    }],
    initialize: function(attributes, options) {
        this.options = options;
        if (!this.id) {
            this.set('id', Common.generateUUID());
        }
        this.listenTo(this.get('uploads'), 'add remove reset update', this.handleUploadUpdate);
        this.listenTo(this.get('uploads'), 'change:issues', this.handleIssuesUpdates);
        this.listenToDropzone();
    },
    listenToDropzone: function() {
        if (this.options.dropzone) {
            this.options.dropzone.on('addedfile', this.handleAddFile.bind(this));
            this.options.dropzone.on('totaluploadprogress', this.handleTotalUploadProgress.bind(this));
            this.options.dropzone.on('sending', this.handleSending.bind(this));
            this.options.dropzone.on('queuecomplete', this.handleQueueComplete.bind(this));
            this.options.dropzone.on('success', this.handleSuccess.bind(this));
            this.options.dropzone.on('error', this.handleError.bind(this));
            this.options.dropzone.on('complete', this.handleComplete.bind(this));
        } else {
            this.set('interrupted', this.get('interrupted') || !this.get('finished'));
            this.set('finished', true);
        }
    },
    handleAddFile: function(file) {
        this.get('uploads').add({
            file: file
        }, {
            dropzone: this.options.dropzone
        });
    },
    handleSuccess: function(file) {
        if (file.status !== 'canceled') {
            this.set('successes', this.get('successes') + 1);
        }
    },
    handleError: function(file) {
        if (file.status !== 'canceled') {
            this.set('errors', this.get('errors') + 1);
        }
    },
    handleComplete: function(file) {
        if (file.status === 'success') {
            this.set('complete', this.get('complete') + 1);
        }
        updatePreferences();
    },
    handleSending: function() {
        this.set({
            sending: true
        });
    },
    handleTotalUploadProgress: function() {
        this.set({
            percentage: calculatePercentageDone(this.options.dropzone.files)
        });
    },
    handleQueueComplete: function() {
        this.set({
            finished: true
        });
        wreqr.vent.trigger('preferences:save');
    },
    handleUploadUpdate: function() {
        this.set({
            amount: this.get('uploads').length
        });
    },
    handleIssuesUpdates: function() {
        this.set({
            issues: this.get('uploads').reduce(function(issues, upload) {
                issues += upload.get('issues') ? 1 : 0;
                return issues;
            }, 0)
        });
    },
    clear: function() {
        this.cancel();
        this.get('uploads').reset();
    },
    cancel: function() {
        if (this.options.dropzone) {
            this.options.dropzone.removeAllFiles(true);
        }
    },
    start: function() {
        if (this.options.dropzone) {
            this.set({
                sending: true,
                sentAt: Date.now() //- Math.random() * 14 * 86400000
            });
            wreqr.vent.trigger('uploads:add', this);
            this.listenTo(this, 'change', updatePreferences);
            this.options.dropzone.options.autoProcessQueue = true;
            this.options.dropzone.processQueue();
        }
    },
    getTimeComparator: function(){
        return this.get('sentAt');
    }
});