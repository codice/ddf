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
var childView = require('./association.view');
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');

module.exports = Marionette.CollectionView.extend({
    childView: childView,
    childViewOptions: function() {
        return {
            selectionInterface: this.options.selectionInterface,
            knownMetacards: this.options.knownMetacards,
            currentMetacard: this.options.currentMetacard
        };
    },
    tagName: CustomElements.register('association-collection'),
    onAddChild: function(childView){
        if (this.$el.hasClass('is-editing')){
            childView.turnOnEditing();
        } else {
            childView.turnOffEditing();
        }
    },
    turnOnEditing: function() {
        this.$el.toggleClass('is-editing', true);
        this.children.forEach(function(childView) {
            childView.turnOnEditing();
        });
    },
    turnOffEditing: function() {
        this.$el.toggleClass('is-editing', false);
        this.children.forEach(function(childView) {
            childView.turnOffEditing();
        });
    }
});