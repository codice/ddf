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
    'marionette',
    'backbone',
    'wreqr',
    'text!templates/geocoder.handlebars',
    'jquery'
], function (_, Marionette, Backbone, wreqr, geocoderTemplate, $) {
    var geocoder = {};
    var url = '/services/REST/v1/Locations';
    var geocoderModel = new Backbone.Model();
    geocoder.View = Marionette.ItemView.extend({
        template: geocoderTemplate,
        events: {
            'keypress #searchfield': 'searchOnEnter',
            'click #searchbutton': 'search'
        },
        initialize: function () {
            this.model = geocoderModel;
            this.modelBinder = new Backbone.ModelBinder();
            this.listenTo(this.model, 'change', this.changedSearchText);
        },
        onRender: function () {
            var searchBinding = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            this.modelBinder.bind(this.model, this.$el, searchBinding);
        },
        searchOnEnter: function (e) {
            if (e.keyCode === 13) {
                //user pushed enter, perform search
                this.model.set('searchText', this.$('#searchfield').val());
                if (this.model.get('searchText')) {
                    this.search();
                }
                e.preventDefault();
            }
        },
        changedSearchText: function () {
            if (this.model.get('searchText')) {
                this.$('#searchfield').addClass('geocoder-input-wide');
            } else {
                this.$('#searchfield').removeClass('geocoder-input-wide');
            }
        },
        search: function () {
            var view = this;
            if (this.model.get('searchText')) {
                $.ajax({
                    url: url,
                    data: 'jsonp=jsonp&query=' + this.model.get('searchText'),
                    success: function (resultStr) {
                        resultStr = resultStr.replace('jsonp(', '');
                        resultStr = resultStr.substring(0, resultStr.length - 1);
                        var result = $.parseJSON(resultStr);
                        if (result.resourceSets.length === 0) {
                            view.model.set('searchText', view.model.get('searchText') + ' (not found)');
                            return;
                        }
                        var resourceSet = result.resourceSets[0];
                        if (resourceSet.resources.length === 0) {
                            view.model.set('searchText', view.model.get('searchText') + ' (not found)');
                            return;
                        }
                        var resource = resourceSet.resources[0];
                        view.model.set('searchText', resource.name);
                        var bbox = resource.bbox;
                        var south = bbox[2];
                        var west = bbox[1];
                        var north = bbox[0];
                        var east = bbox[3];
                        wreqr.vent.trigger('search:maprectanglefly', {
                            north: north,
                            south: south,
                            east: east,
                            west: west
                        });
                    },
                    error: function (data) {
                        view.model.set('searchText', data);
                    }
                });
            }
        }
    });
    return geocoder;
});
