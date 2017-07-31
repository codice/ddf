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
define([
    'marionette',
    'underscore',
    'jquery',
    './query-title.hbs',
    'js/CustomElements',
    'js/store'
], function(Marionette, _, $, template, CustomElements, store) {

    var zeroWidthSpace = "\u200B";

    return Marionette.ItemView.extend({
        events: {
            'change input': 'updateQueryName',
            'keyup input': 'updateQueryName',
            'click .is-button': 'focus'
        },
        template: template,
        tagName: CustomElements.register('query-title'),
        initialize: function(options) {
            this.updateQueryName = _.throttle(this.updateQueryName, 200);
            if (options.model === undefined) {
                this.setDefaultModel();
            }
        },
        onDomRefresh: function() {
            if (!this.model._cloneOf) {
                this.$el.find('input').select();
            }
        },
        onRender: function(){
            this.updateQueryName();
        },
        focus: function(){
            this.$el.find('input').focus();
        },
        getSearchTitle: function(){
            var title = this.$el.find('input').val();
            return title !== "" ? title : 'Search Title';
        },
        updateQueryName: function(e) {
            this.$el.find('.button-title').html(this.getSearchTitle() + zeroWidthSpace);
            this.save();
        },
        save: function(){
            this.model.set('title', this.$el.find('input').val());
        }
    });
});