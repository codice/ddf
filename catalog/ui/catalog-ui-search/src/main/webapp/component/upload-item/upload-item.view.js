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
/*global require, setTimeout*/
var wreqr = require('wreqr');
var Marionette = require('marionette');
var _ = require('underscore');
var $ = require('jquery');
var template = require('./upload-item.hbs');
var CustomElements = require('js/CustomElements');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('upload-item'),
    events: {
        'click .upload-cancel': 'cancelUpload',
        'click .upload-expand': 'expandUpload',
        'click': 'expandIfSuccess'
    },
    modelEvents: {
        'change:percentage': 'handlePercentage',
        'change:sending': 'handleSending',
        'change:success': 'handleSuccess',
        'change:error': 'handleError',
        'change:validating': 'handleValidating',
        'change:issues': 'handleIssues'
    },
    initialize: function() {},
    onRender: function() {
        this.handleSending();
        this.handlePercentage();
        this.handleError();
        this.handleSuccess();
        this.handleIssues();
        this.handleValidating();
    },
    handleSending: function() {
        var sending = this.model.get('sending');
        this.$el.toggleClass('show-progress', sending);
    },
    handlePercentage: function() {
        var percentage = this.model.get('percentage');
        this.$el.find('.info-progress').css('width', percentage + '%');
        this.$el.find('.bottom-percentage').html(Math.floor(percentage) + '%');
    },
    handleError: function() {
        var error = this.model.get('error');
        this.$el.toggleClass('has-error', error);
        this.$el.find('.error-message').html(this.model.escape('message'));
    },
    handleSuccess: function(file, response) {
        var success = this.model.get('success');
        this.$el.toggleClass('has-success', success);
        this.$el.find('.success-message .message-text').html(this.model.escape('message'));
        this.handleChildren();
    },
    handleChildren() {
        this.$el.toggleClass('has-children', this.model.hasChildren());
    },
    handleValidating: function() {
        var validating = this.model.get('validating');
        this.$el.toggleClass('checking-validation', validating);
    },
    handleIssues: function() {
        var issues = this.model.get('issues');
        this.$el.toggleClass('has-validation-issues', issues);
    },
    serializeData: function() {
        var modelJSON = this.model.toJSON();
        modelJSON.file = {
            name: modelJSON.file.name,
            size: (modelJSON.file.size / 1000000).toFixed(2) + 'MB, ',
            type: modelJSON.file.type
        };
        return modelJSON;
    },
    cancelUpload: function() {
        this.cancelUpload = $.noop;
        this.$el.toggleClass('is-removed', true);
        setTimeout(function() {
            this.model.cancel();
        }.bind(this), 250);
    },
    expandUpload: function() {
        wreqr.vent.trigger('router:navigate', {
            fragment: 'metacards/' + this.model.get('id'),
            options: {
                trigger: true
            }
        });
    },
    expandIfSuccess: function(){
        if (this.model.get('success') && !this.model.hasChildren()){
            this.expandUpload();
        }
    }
});