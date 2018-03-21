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
/*global require, window*/
var template = require('./table.hbs');
var Marionette = require('marionette');
var MarionetteRegion = require('js/Marionette.Region');
var CustomElements = require('js/CustomElements');

function moveHeaders(elementToUpdate, elementToMatch) {
    this.$el.find('th').css('transform', 'translate3d(0, '+ this.el.scrollTop+'px, 0)');
}

module.exports = Marionette.LayoutView.extend({
    tagName: CustomElements.register('table'),
    template: template,
    regions: {
        bodyThead: {
            selector: 'thead',
            replaceElement: true
        },
        bodyTbody: {
            selector: 'tbody',
            replaceElement: true
        }
    },
    headerAnimationFrameId: undefined,
    getHeaderView: function(){
        console.log('You need to overwrite this function and provide the constructed HeaderView');
    },
    getBodyView: function(){
        console.log('You need to overwrite this function and provide the constructed BodyView');
    },
    onRender: function() {
        this.bodyTbody.show(this.getBodyView(), {
            replaceElement: true
        });
        this.bodyThead.show(this.getHeaderView(), {
            replaceElement: true
        });
        this.onDestroy();
        this.startUpdatingHeaders();
    },
    startUpdatingHeaders: function(){
        window.requestAnimationFrame(function(){
            moveHeaders.call(this);
            this.startUpdatingHeaders();
        }.bind(this));
    },
    onDestroy: function(){
        window.cancelAnimationFrame(this.headerAnimationFrameId);
    }
});