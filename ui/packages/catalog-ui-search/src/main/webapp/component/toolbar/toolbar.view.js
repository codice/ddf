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
var template = require('./toolbar.hbs');
var CustomElements = require('CustomElements');
var $ = require('jquery');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('toolbar'),
    events: {
        'click #item-add-visual': 'toggleAddVisual',
        'click #item-toggle-search': 'toggleSearchPanel'
    },
    initialize: function() {
    },
    onBeforeShow: function(){
    },
    toggleSearchPanel: function(){
        $('#item-toggle-search').toggleClass('is-open');
        this.triggerMethod('content:togglePanelOne');
    },
    toggleAddVisual: function(){
        $('#item-add-visual').toggleClass('is-open');
        this.triggerMethod('content:togglePanelTwo');
    }
});
