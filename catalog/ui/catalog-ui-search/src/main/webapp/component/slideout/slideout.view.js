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
var template = require('./slideout.hbs');
var CustomElements = require('js/CustomElements');
var $ = require('jquery');

var componentName = 'slideout';

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register(componentName),
    events: {
        'click': 'handleOutsideClick'
    },
    regions: {
        'slideoutContent': '.slideout-content'
    },
    initialize: function() {
        $('body').append(this.el);
        this.listenForClose();
    },
    listenForClose: function() {
        this.$el.on(CustomElements.getNamespace() + 'close-' + componentName, function() {
            this.close();
        }.bind(this));
    },
    open: function() {
        this.$el.toggleClass('is-open', true);
    },
    handleOutsideClick: function(event) {
        if (event.target === this.el) {
            this.close();
        }
    },
    close: function() {
        this.$el.toggleClass('is-open', false);
    },
    updateContent: function(view){
        this.slideoutContent.show(view);
    }
});