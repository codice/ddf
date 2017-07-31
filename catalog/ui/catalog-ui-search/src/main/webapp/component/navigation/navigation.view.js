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
var template = require('./navigation.hbs');
var CustomElements = require('CustomElements');
var NavigationLeftView = require('component/navigation-left/navigation-left.view');
var NavigationRightView = require('component/navigation-right/navigation-right.view');
var store = require('js/store');
var wreqr = require('wreqr');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('navigation'),
    events: {
        'mousedown > .cancel-drawing': 'handleCancelDrawing'
    },
    regions: {
        navigationLeft: '.navigation-left',
        navigationMiddle: '.navigation-middle',
        navigationRight: '.navigation-right'
    },
    showNavigationMiddle: function(){
        //override in extensions
    },
    onBeforeShow: function(){
        this.navigationLeft.show(new NavigationLeftView());
        this.showNavigationMiddle();
        this.navigationRight.show(new NavigationRightView());
    },
    handleCancelDrawing: function(e){
        e.stopPropagation();
        wreqr.vent.trigger('search:drawend', store.get('content').get('drawingModel'));
    }
});