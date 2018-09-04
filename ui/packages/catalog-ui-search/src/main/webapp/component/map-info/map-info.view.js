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
var template = require('./map-info.hbs');
var CustomElements = require('js/CustomElements');
var mtgeo = require('mt-geo');
var user = require('component/singletons/user-instance'); 

function getCoordinateFormat(){
    return user.get('user').get('preferences').get('coordinateFormat');
}

function leftPad(numToPad, size) {
    var sign = Math.sign(numToPad) === -1 ? '-' : '';
    return new Array(sign === '-' ? size - 1 : size).concat([numToPad]).join(' ').slice(-size);
}

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('map-info'),
    modelEvents: {},
    events: {
        'click > .user-settings-navigation .navigation-choice': 'handleNavigate',
    },
    regions: {
    },
    ui: {},
    initialize: function() {
        this.listenTo(this.model, 'change:mouseLat change:mouseLon change:target', this.render);
        this.listenTo(this.model, 'change:target', this.handleTarget);
        this.listenTo(user.get('user').get('preferences'), 'change:coordinateFormat', this.render);
    },
    onBeforeShow: function() {
    },
    handleBack: function(){
    },
    handleTarget: function() {
        this.$el.toggleClass('has-feature', this.model.get('target') !== undefined);
    },
    serializeData: function(){
        let modelJSON = this.model.toJSON();
        let viewData = {
            target: modelJSON.target,
            lat: modelJSON.mouseLat,
            lon: modelJSON.mouseLon
        };
        switch(getCoordinateFormat()){
            case 'degrees':
                viewData.lat = typeof viewData.lat === 'undefined' ? viewData.lat : mtgeo.toLat(viewData.lat);
                viewData.lon = typeof viewData.lon === 'undefined' ? viewData.lon : mtgeo.toLon(viewData.lon);
            break;
            case 'decimal':
                viewData.lat = typeof viewData.lat === 'undefined' ? viewData.lat :
                    `${leftPad(Math.floor(viewData.lat),3,' ')}.${Math.abs(viewData.lat%1).toFixed(6).toString().slice(2)}`;
                viewData.lon = typeof viewData.lon === 'undefined' ? viewData.lon :
                    `${leftPad(Math.floor(viewData.lon),4,' ')}.${Math.abs(viewData.lon%1).toFixed(6).toString().slice(2)}`;
            break;
        }
        return viewData;
    },
    onRender: function() {
        this.$el.toggleClass('is-off-map', typeof this.model.get('mouseLat') === 'undefined');
    }
});