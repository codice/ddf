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
var Marionette = require('marionette');
var template = require('./list-interactions.hbs');
var CustomElements = require('js/CustomElements');

module.exports = Marionette.ItemView.extend({
    template: template,
    tagName: CustomElements.register('list-interactions'),
    className: 'composed-menu',
    events: {
        'click .interaction-run': 'triggerRun',
        'click .interaction-stop': 'triggerCancel',
        'click .interaction-delete': 'triggerDelete',
        'click .interaction-duplicate': 'triggerDuplicate',
        'click': 'triggerClick'
    },
    initialize: function(){
        if (!this.model.get('query').get('result')) {
            this.startListeningToSearch();
        }
        this.handleResult();
    },
    startListeningToSearch: function(){
        this.listenToOnce(this.model.get('query'), 'change:result', this.startListeningForResult);
    },
    startListeningForResult: function(){
        this.listenToOnce(this.model.get('query').get('result'), 'sync error', this.handleResult);
    },
    triggerRun: function(){
        this.model.get('query').startSearch();
    },
    triggerCancel: function(){
        this.model.get('query').cancelCurrentSearches();
    },
    triggerDelete: function(){
        this.model.collection.remove(this.model);
    },
    triggerDuplicate: function(){
        var copyAttributes = JSON.parse(JSON.stringify(this.model.attributes));
        delete copyAttributes.id;
        delete copyAttributes.query;
        var newList = new this.model.constructor(copyAttributes);
        this.model.collection.add(newList);
    },
    handleResult: function(){
        this.$el.toggleClass('has-results', this.model.get('query').get('result') !== undefined);
    },
    triggerClick: function(){
        this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
    }
});