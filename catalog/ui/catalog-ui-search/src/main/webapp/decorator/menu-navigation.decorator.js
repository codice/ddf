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
/*global define*/

module.exports = {
    events: {
        'mouseenter > *': 'handleMouseEnter'
    },
    handleEnter: function() {
        this.$el.children('.is-active').click();
    },
    handleUpArrow: function() {
        var currentActive = this.$el.children('.is-active');
        var potentialNext = currentActive.prevAll().filter(function(index, element) {
            return element.offsetParent !== null;
        }).first()
        if (potentialNext.length > 0) {
            currentActive.removeClass('is-active');
            potentialNext.addClass('is-active');
        }
    },
    handleDownArrow: function() {
        var currentActive = this.$el.children('.is-active');
        var potentialNext = currentActive.nextAll().filter(function(index, element) {
            return element.offsetParent !== null;
        }).first()
        if (potentialNext.length > 0) {
            currentActive.removeClass('is-active');
            potentialNext.addClass('is-active');
        }
    },
    handleMouseEnter: function(e) {
        this.$el.children('.is-active').removeClass('is-active');
        this.$el.find(e.currentTarget).toggleClass('is-active');
    }
}