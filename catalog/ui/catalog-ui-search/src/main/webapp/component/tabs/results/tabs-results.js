/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
define([
    'underscore',
    '../tabs',
    'js/store',
    'component/result-selector/result-selector.view',
    'component/result-aggregator/result-aggregator.view'
], function (_, Tabs, store, ResultSelectorView, ResultAggregatorView) {

    var Tab = function(color, getModel, getView, icon, tooltip){
        this.color = color;
        this.getModel = getModel;
        this.getView = getView;
        this.icon = icon;
        this.tooltip = tooltip;
        this.filter = false;
    };

    var WorkspaceContentTabs = Tabs.extend({
        defaults: function() {
            return {
                tabs: {
                    'By Time': {
                        color: undefined,
                        getModel: function () {
                            return undefined;
                        },
                        getView: function () {
                            return ResultAggregatorView;
                        },
                        icon: 'fa-clock-o',
                        filter: false
                    }
                }
            };
        },
        addTab: function(id, options){
            this.get('tabs')[id] = new Tab(options.color, options.getModel, options.getView, options.icon, options.tooltip);
        },
        removeTab: function(id){
            delete this.get('tabs')[id];
        },
        filterTab: function(id){
            this.get('tabs')[id].filter = true;
        },
        unfilterTab: function(id){
            this.get('tabs')[id].filter = false;
        }
    });

    return WorkspaceContentTabs;
});