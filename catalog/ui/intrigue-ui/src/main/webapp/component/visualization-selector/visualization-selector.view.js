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
var template = require('./visualization-selector.hbs');
var CustomElements = require('js/CustomElements');
var user = require('component/singletons/user-instance');

module.exports = Marionette.ItemView.extend({
    template: template,
    tagName: CustomElements.register('visualization-selector'),
    events: {
        'click .choice-2dmap': 'handle2dmap',
        'click .choice-3dmap': 'handle3dmap',
        'click .choice-histogram': 'handleHistogram',
        'click .choice-table': 'handleTable',
        'click .visualization-choice': 'handleChoice'
    },
    handle2dmap: function(){
        user.get('user').get('preferences').set('visualization', '2dmap');
    },
    handle3dmap: function(){
        user.get('user').get('preferences').set('visualization', '3dmap');
    },
    handleHistogram: function(){
        user.get('user').get('preferences').set('visualization', 'histogram');
    },
    handleTable: function(){
        user.get('user').get('preferences').set('visualization', 'table');
    },
    handleChoice: function(){
        this.$el.trigger('closeSlideout.' + CustomElements.getNamespace());
    }
});