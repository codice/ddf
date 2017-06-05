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
    'js/store'
], function (Marionette, _, $, TabsView, store) {

    var WorkspaceContentTabsView = TabsView.extend({
        initialize: function(){
            TabsView.prototype.initialize.call(this);
            this.listenTo(this.model, 'change:activeTab', this.closePanelTwo);
        },
        closePanelTwo: function(){
            store.get('content').set('query', undefined);
            this.options.selectionInterface.setCurrentQuery(undefined);
            this.options.selectionInterface.setActiveSearchResults([]);
            this.options.selectionInterface.clearSelectedResults();
        },
        onDestroy: function(){
            this.closePanelTwo();
        }
    });

    return WorkspaceContentTabsView;
});