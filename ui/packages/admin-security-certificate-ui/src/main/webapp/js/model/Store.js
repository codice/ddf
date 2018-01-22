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
        'jquery',
        'backbone',
        'backboneassociations'
    ],
    function ($, Backbone) {
        var Store = {};

        Store.Response = Backbone.AssociatedModel.extend({

            removeStoreItem: function (url, alias) {
                var that = this;
                $.ajax({
                    url: url + alias,
                    type: 'GET'
                }).then(function () {
                    that.trigger('reload');
                });
            },
            addStoreItem: function (url, alias, data) {
                var that = this;
                var jData = JSON.stringify(data);
                $.ajax({
                    url: url + alias,
                    dataType: 'json',
                    data: jData
                }).then(function () {
                    that.trigger('reload');
                });
            }

        });
        return Store;

    });
