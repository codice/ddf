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
var Marionette = require('marionette');
var _ = require('underscore');
var $ = require('jquery');
var template = require('./metacard-preview.hbs');
var CustomElements = require('js/CustomElements');
var LoadingCompanionView = require('component/loading-companion/loading-companion.view');
var store = require('js/store');
var user = require('component/singletons/user-instance.js');
var preferences = user.get('user').get('preferences');

function getSrc(previewHtml) {
    return '<html class="is-iframe is-preview" style="font-size: '+preferences.get('fontSize')+'px">' +
    '<link href="css/styles.' + document.querySelector('link[href*="css/styles."]').href.split('css/styles.')[1] + '" rel="stylesheet">' +
    previewHtml +
    '</html>';
}

module.exports = Marionette.ItemView.extend({
    setDefaultModel: function() {
        this.model = this.selectionInterface.getSelectedResults().first();
    },
    template: template,
    tagName: CustomElements.register('metacard-preview'),
    selectionInterface: store,
    initialize: function(options) {
        this.selectionInterface = options.selectionInterface || this.selectionInterface;
        if (!options.model) {
            this.setDefaultModel();
        }
        LoadingCompanionView.beginLoading(this);
        this.previewRequest = $.get({
            url: this.model.getPreview(),
            dataType: 'html',
            customErrorHandling: true
        }).then(function(previewHtml) {
            this.previewHtml = previewHtml;
        }.bind(this)).always(function() {
            LoadingCompanionView.endLoading(this);
        }.bind(this));
    },
    onAttach: function(){
        this.previewRequest.then(function(){
            if (!this.isDestroyed) {
                this.populateIframe();
                this.listenTo(user.get('user').get('preferences'), 'change:fontSize', this.populateIframe);
            }
        }.bind(this));
    },
    populateIframe: function(){
        var $iframe = this.$el.find('iframe');
        $iframe.ready(function(){
            $iframe.contents()[0].open();
            $iframe.contents()[0].write(getSrc(this.previewHtml));
            $iframe.contents()[0].close();
        }.bind(this));
    },
    onDestroy: function(){
    }
});