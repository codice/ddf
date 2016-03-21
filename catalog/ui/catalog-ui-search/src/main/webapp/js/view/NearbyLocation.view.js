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
    'jquery',
    'underscore',
    'marionette',
    'text!templates/metacardNearbyLocation.handlebars'
], function ($, _, Marionette, metacardNearbyLocationTemplate) {
    'use strict';
    var NearbyLocationView = {};
    NearbyLocationView = Marionette.ItemView.extend({
        template: metacardNearbyLocationTemplate,
        modelEvents: { 'change': 'onRender' },
        initialize: function () {
            this.model.fetch();
        },
        onRender: function () {
            this.$el.html(this.getNearby());
        },
        getNearby: function () {
            if (this.model.get('name') !== '' && this.model.get('distance') !== '' && this.model.get('direction') !== '') {
                var float = parseFloat(this.model.get('distance'));
                return float.toFixed(3) + ' km ' + this.model.get('direction') + ' of ' + this.model.get('name');
            }
        }
    });
    return NearbyLocationView;
});
