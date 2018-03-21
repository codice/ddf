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
var _ = require('underscore');
var _merge = require('lodash/merge');
var _debounce = require('lodash/debounce');
var $ = require('jquery');
var wreqr = require('wreqr');
var template = require('./golden-layout.hbs');
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');
var GoldenLayout = require('golden-layout');
var properties = require('properties');
var TableView = require('component/visualization/table/table-viz.view');
var InspectorView = require('component/visualization/inspector/inspector.view');
var OpenlayersView = require('component/visualization/maps/openlayers/openlayers.view');
var HistogramView = require('component/visualization/histogram/histogram.view');
var CombinedMapView = require('component/visualization/combined-map/combined-map.view');
var LowBandwidthMapView = require('component/visualization/low-bandwidth-map/low-bandwidth-map.view');
var Common = require('js/Common');
var store = require('js/store');
var user = require('component/singletons/user-instance');
var VisualizationDropdown = require('component/dropdown/visualization-selector/dropdown.visualization-selector.view');
var DropdownModel = require('component/dropdown/dropdown');
const sanitize = require('sanitize-html');

const treeMap = (obj, fn, path = []) => {
    if (Array.isArray(obj)) {
        return obj.map((v, i) => treeMap(v, fn, path.concat(i)));
    }

    if (obj !== null && typeof obj === 'object') {
        return Object.keys(obj)
            .map((k) => [k, treeMap(obj[k], fn, path.concat(k))])
            .reduce((o, [k, v]) => {
                o[k] = v;
                return o;
            }, {});
    }

    return fn(obj, path);
};

const sanitizeTree = (tree) => treeMap(tree, (obj) => {
    if (typeof obj === 'string') {
        return sanitize(obj, {
            allowedTags: [],
            allowedAttributes: []
        });
    }
    return obj;
});

var defaultGoldenLayoutContent = {
    content: properties.defaultLayout || [{
        type: 'stack',
        content: [{
            type: 'component',
            componentName: 'cesium',
            title: '3D Map'
        }, {
            type: 'component',
            componentName: 'inspector',
            title: 'Inspector'
        }]
    }]
};

function getGoldenLayoutSettings(){
    var minimumScreenSize = 20; //20 rem or 320px at base font size
    var fontSize = parseInt(user.get('user').get('preferences').get('fontSize'));
    var theme = user.get('user').get('preferences').get('theme').getTheme();
    return {
        settings: {
            showPopoutIcon: false,
            responsiveMode: 'none'
        },
        dimensions: {
            borderWidth: 0.5 * parseFloat(theme.minimumSpacing) * fontSize,
            minItemHeight: minimumScreenSize * fontSize,
            minItemWidth: minimumScreenSize * fontSize,
            headerHeight: parseFloat(theme.minimumButtonSize) * fontSize,
            dragProxyWidth: 300,
            dragProxyHeight: 200
        }
    };
}

function registerComponent(marionetteView, name, ComponentView) {
    registerComponent(marionetteView, name, ComponentView, {});
}
// see https://github.com/deepstreamIO/golden-layout/issues/239 for details on why the setTimeout is necessary
// The short answer is it mostly has to do with making sure these ComponentViews are able to function normally (set up events, etc.)
function registerComponent(marionetteView, name, ComponentView, componentOptions) {
    var options = _.extend({}, marionetteView.options, componentOptions);
    marionetteView.goldenLayout.registerComponent(name, function (container, componentState) {
        container.on('open', () => {
            setTimeout(function () {
                var componentView = new ComponentView(_.extend({}, options, componentState, {
                    container: container
                }));
                container.getElement().append(componentView.el);
                componentView.render();
                container.on('destroy', () => {
                    componentView.destroy();
                });
            }, 0);
        });
        container.on('tab', (tab) => {
            tab.closeElement.off('click').on('click', (event) => {
                if (tab.header.parent.isMaximised && tab.header.parent.contentItems.length === 1){
                    tab.header.parent.toggleMaximise();
                } 
                tab._onCloseClickFn(event);
            });
        });
    });
}

function unMaximize(contentItem) {
    if (contentItem.isMaximised) {
        contentItem.toggleMaximize();
        return true;
    } else if (contentItem.contentItems.length === 0) {
        return false;
    } else {
        return _.some(contentItem.contentItems, isMaximised);
    }
}

function isMaximised(contentItem) {
    if (contentItem.isMaximised) {
        return true;
    } else if (contentItem.contentItems.length === 0) {
        return false;
    } else {
        return _.some(contentItem.contentItems, isMaximised);
    }
}

function removeActiveTabInformation(config){
    if (config.activeItemIndex !== undefined) {
        config.activeItemIndex = 0;
    }
    if (config.content === undefined || config.content.length === 0){
        return;
    } else {
        return _.forEach(config.content, removeActiveTabInformation);
    }
}

function removeMaximisedInformation(config){
    delete config.maximisedItemId;
}

function removeEphemeralState(config){
    removeMaximisedInformation(config);
    removeActiveTabInformation(config);
}

module.exports = Marionette.LayoutView.extend({
    tagName: CustomElements.register('golden-layout'),
    template: template,
    className: 'is-minimised',
    events: {
        'click > .golden-layout-toolbar .to-toggle-size': 'handleToggleSize',
    },
    regions: {
        toolbar: '> .golden-layout-toolbar',
        widgetDropdown: '> .golden-layout-toolbar .to-add'
    },
    initialize: function (options) {
        this.options.selectionInterface = options.selectionInterface || store;
    },
    updateFontSize: function () {
        var goldenLayoutSettings = getGoldenLayoutSettings();
        this.goldenLayout.config.dimensions.borderWidth = goldenLayoutSettings.dimensions.borderWidth;
        this.goldenLayout.config.dimensions.minItemHeight = goldenLayoutSettings.dimensions.minItemHeight;
        this.goldenLayout.config.dimensions.minItemWidth = goldenLayoutSettings.dimensions.minItemWidth;
        this.goldenLayout.config.dimensions.headerHeight = goldenLayoutSettings.dimensions.headerHeight;
        Common.repaintForTimeframe(2000, () => {
            this.goldenLayout.updateSize();
        });
    },
    updateSize: function () {
        this.goldenLayout.updateSize();
    },
    showWidgetDropdown: function () {
        this.widgetDropdown.show(new VisualizationDropdown({
            model: new DropdownModel(),
            goldenLayout: this.goldenLayout
        }));
    },
    showGoldenLayout: function () {
        this.goldenLayout = new GoldenLayout(this.getGoldenLayoutConfig(), this.el.querySelector('.golden-layout-container'));
        this.registerGoldenLayoutComponents();
        this.goldenLayout.on('stateChanged', _.debounce(this.handleGoldenLayoutStateChange.bind(this), 200));
        this.goldenLayout.on('stackCreated', this.handleGoldenLayoutStackCreated);
        this.goldenLayout.on('initialised', this.handleGoldenLayoutInitialised.bind(this));
        this.goldenLayout.init();
    },
    getGoldenLayoutConfig: function(){
        var currentConfig = user.get('user').get('preferences').get(this.options.configName);
        if (currentConfig === undefined){
            currentConfig = defaultGoldenLayoutContent;
        }
        _merge(currentConfig, getGoldenLayoutSettings());
        return sanitizeTree(currentConfig);
    },
    registerGoldenLayoutComponents: function(){
        registerComponent(this, 'inspector', InspectorView);
        registerComponent(this, 'table', TableView);
        registerComponent(this, 'cesium', LowBandwidthMapView, {desiredContainer: 'cesium'});
        registerComponent(this, 'histogram', HistogramView);
        registerComponent(this, 'openlayers', LowBandwidthMapView, {desiredContainer: 'openlayers'});
    },
    detectIfGoldenLayoutMaximised: function(){
        this.$el.toggleClass('is-maximised', isMaximised(this.goldenLayout.root));
    },
    detectIfGoldenLayoutEmpty: function(){
        this.$el.toggleClass('is-empty', this.goldenLayout.root.contentItems.length === 0);
    },
    handleGoldenLayoutInitialised: function(){
        this.detectIfGoldenLayoutMaximised();
        this.detectIfGoldenLayoutEmpty();
    },
    handleGoldenLayoutStackCreated: function(stack) {
        stack.header.controlsContainer.find('.lm_close').off('click').on('click', (event) => {
            if (stack.isMaximised){
                stack.toggleMaximise();
            }
            stack.remove();
        });
    },
    handleGoldenLayoutStateChange: function(event){
        this.detectIfGoldenLayoutMaximised();
        this.detectIfGoldenLayoutEmpty();
        //https://github.com/deepstreamIO/golden-layout/issues/253
        if (this.goldenLayout.isInitialised) {
            var currentConfig = this.goldenLayout.toConfig();
            removeEphemeralState(currentConfig);
            user.get('user').get('preferences').set(this.options.configName, currentConfig);
            wreqr.vent.trigger('resize');
            //do not add a window resize event, that will cause an endless loop.  If you need something like that, listen to the wreqr resize event.
        }
    },
    setupListeners: function () {
        this.listenTo(user.get('user').get('preferences'), 'change:theme', this.updateFontSize);
        this.listenTo(user.get('user').get('preferences'), 'change:fontSize', this.updateFontSize);
        this.listenForResize();
    },
    onRender: function () {
        this.showGoldenLayout();
        this.showWidgetDropdown();
        this.setupListeners();
    },
    handleToggleSize: function () {
        this.$el.toggleClass('is-minimised');
        this.goldenLayout.updateSize();
    },
    listenForResize: function () {
        $(window).on('resize.' + this.cid, _debounce((event) => {
            this.updateSize();
        }, 100, {
            leading: false,
            trailing: true
        }));
    },
    stopListeningForResize: function () {
        $(window).off('resize.' + this.cid);
    },
    onDestroy: function () {
        this.stopListeningForResize();
        if (this.goldenLayout){
            this.goldenLayout.destroy();
        }
    }
});