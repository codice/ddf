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
var template = require('./unsaved-indicator.hbs');
var CustomElements = require('js/CustomElements');

// Base View, meant to be extended for whatever needs an unsaved indicator
module.exports = Marionette.ItemView.extend({
    template: template,
    tagName: CustomElements.register('unsaved-indicator'),
    initialize: function() {
        // overwrite with listeners
    },
    onBeforeShow: function() {
        this.handleSaved();
    },
    isSaved: function() {
        // overwrite with save check
    },
    handleSaved: function() {
        this.$el.toggleClass('is-saved', this.isSaved());
    }
});