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
/*global define*/
define([
    'marionette',
    'underscore',
    'jquery',
    './alert-item.view',
    'js/CustomElements',
    'component/singletons/user-instance'
], function (Marionette, _, $, childView, CustomElements, user) {

    return Marionette.CollectionView.extend({
        emptyView: Marionette.ItemView.extend({className: 'alert-empty', template: 'No Recent Alerts'}),
        className: 'is-list has-list-highlighting',
        setDefaultCollection: function(){
            this.collection = user.get('user').get('preferences').get('alerts');
        },
        childView: childView,
        tagName: CustomElements.register('alert-item-collection'),
        initialize: function(options){
            if (!options.collection){
                this.setDefaultCollection();
            }
        }
    });
});
