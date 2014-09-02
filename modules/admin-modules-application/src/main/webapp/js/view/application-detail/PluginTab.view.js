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
/* global define */
define([
    'marionette',
    'icanhaz',
    'text!pluginTabItemView',
    'text!pluginTabCollectionView'
    ],function (Marionette, ich, pluginTabItemView, pluginTabCollectionView) {

    ich.addTemplate('pluginTabItemView',pluginTabItemView);
    ich.addTemplate('pluginTabCollectionView', pluginTabCollectionView);

    var ItemView = Marionette.ItemView.extend({
        template: 'pluginTabItemView',
        tagName: 'li',
        className: 'itemview'
    });

    var PluginTabView = Marionette.CompositeView.extend({
        template: 'pluginTabCollectionView',
        itemView: ItemView,
        className: '.plugin-view',
        itemViewContainer: '.nav.nav-tabs'
    });


    return PluginTabView;
});