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
/*global define, alert*/
define([
    'marionette',
    'underscore',
    'jquery',
    '../tabs.view',
    './tabs-metacards',
    'js/store'
], function (Marionette, _, $, TabsView, MetacardsTabsModel, store) {

    var MetacardsTabsView = TabsView.extend({
        setDefaultModel: function(){
            this.model = new MetacardsTabsModel();
        },
        selectionInterface: store,
        initialize: function(options){
            this.selectionInterface = options.selectionInterface || store;
            if (options.model === undefined){
                this.setDefaultModel();
            }
            TabsView.prototype.initialize.call(this)
            var debounceDetermineContent = _.debounce(this.determineContent, 200);
            this.listenTo(this.selectionInterface.getSelectedResults(), 'update',debounceDetermineContent);
            this.listenTo(this.selectionInterface.getSelectedResults(), 'add', debounceDetermineContent);
            this.listenTo(this.selectionInterface.getSelectedResults(), 'remove', debounceDetermineContent);
            this.listenTo(this.selectionInterface.getSelectedResults(), 'reset', debounceDetermineContent);
        },
        determineContent: function(){
            if (this.selectionInterface.getSelectedResults().length > 1) {
                var activeTab = this.model.getActiveView();
                this.tabsContent.show(new activeTab({
                    selectionInterface: this.selectionInterface
                }));
            }
        }
    });

    return MetacardsTabsView;
});