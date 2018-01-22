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
var _ = require('underscore');
var template = require('./visualization-selector.hbs');
var CustomElements = require('js/CustomElements');
var user = require('component/singletons/user-instance');

var configs = {
    openlayers: {
        title: '2D Map',
        type: 'component',
        componentName: 'openlayers',
        componentState: { }
    },
    cesium: {
        title: '3D Map',
        type: 'component',
        componentName: 'cesium',
        componentState: { }
    },
    inspector: {
        title: 'Inspector',
        type: 'component',
        componentName: 'inspector',
        componentState: { }
    },
    table: {
        title: 'Table',
        type: 'component',
        componentName: 'table',
        componentState: { }
    },
    histogram: {
        title: 'Histogram',
        type: 'component',
        componentName: 'histogram',
        componentState: {}
    }
};

function unMaximize(contentItem){
    if (contentItem.isMaximised){
        contentItem.toggleMaximise();
        return true;
    } else if (contentItem.contentItems.length === 0) {
        return false;
    } else {
        return _.some(contentItem.contentItems, (subContentItem) => {
            return unMaximize(subContentItem);
        });
    }
}

module.exports = Marionette.ItemView.extend({
    template: template,
    tagName: CustomElements.register('visualization-selector'),
    events: {
        'click .visualization-choice': 'handleChoice',
        'mousedown .visualization-choice': 'handleMouseDown',
        'mouseup .visualization-choice': 'handleMouseUp'
    },
    dragSources: [],
    onRender: function() {
        this.dragSources = [];
        this.dragSources.push(this.options.goldenLayout.createDragSource(this.el.querySelector('.choice-2dmap'), configs.openlayers));
        this.dragSources.push(this.options.goldenLayout.createDragSource(this.el.querySelector('.choice-3dmap'), configs.cesium));
        this.dragSources.push(this.options.goldenLayout.createDragSource(this.el.querySelector('.choice-histogram'), configs.histogram));
        this.dragSources.push(this.options.goldenLayout.createDragSource(this.el.querySelector('.choice-table'), configs.table));
        this.dragSources.push( this.options.goldenLayout.createDragSource(this.el.querySelector('.choice-inspector'), configs.inspector));
        this.listenToDragSources();
    },
    listenToDragStart: function(dragSource){
        dragSource._dragListener.on('dragStart', () => {
            this.interimState = false;
        });
    },
    listenToDragStop: function(dragSource){
        dragSource._dragListener.on('dragStop', () => {
            this.listenToDragStart(dragSource);
            this.listenToDragStop(dragSource);
        });
    },
    listenToDragSources: function(){
        this.dragSources.forEach((dragSource) => {
            this.listenToDragStart(dragSource);
            this.listenToDragStop(dragSource);
        });
    },
    handleChoice: function(){
        this.$el.trigger('closeSlideout.' + CustomElements.getNamespace());
    },
    handleMouseDown: function(event){
        unMaximize(this.options.goldenLayout.root);
        this.interimState = true;
        this.interimChoice = event.currentTarget.getAttribute('data-choice');
    },
    handleMouseUp: function(){
        if (this.interimState){
            if (this.options.goldenLayout.root.contentItems.length === 0){
                this.options.goldenLayout.root.addChild({
                    type: 'column',
                    content: [configs[this.interimChoice]]
                });
            } else {
                this.options.goldenLayout.root.contentItems[0].addChild(configs[this.interimChoice]);
            }
        }
        this.interimState = false;
    }
});