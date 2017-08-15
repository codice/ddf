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
var template = require('./input-textarea.hbs');
var InputView = require('../input.view');

module.exports = InputView.extend({
    template: template,
    events: {
        'click .ta-expand': 'isOverflowing'
    },
    getCurrentValue: function () {
        return this.$el.find('textarea').val();
    },
    isOverflowing: function () {
        var textarea = this.$el.find(".ta-disabled");
        var scrollableHeight = textarea.prop("scrollHeight");
        var currViewableHeight = parseInt(textarea.css("max-height"), 10);

        if (this.$el.hasClass("is-expanded")) {
            this.$el.toggleClass("is-expanded", false);
            textarea.css("height", "75px");
            textarea.css("max-height", "75px");
        } else {
            if (scrollableHeight > currViewableHeight) {
                this.$el.toggleClass("is-expanded", true);
                textarea.css("height", scrollableHeight + 15);
                textarea.css("max-height", scrollableHeight + 15);
            }
        }

    }
});