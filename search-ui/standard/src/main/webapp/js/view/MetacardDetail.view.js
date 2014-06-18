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
        'backbone',
        'icanhaz',
        'direction',
        'maptype',
        'wreqr',
        'cometdinit',
        'text!templates/metacard.handlebars'
    ],
    function ($, _, Backbone, ich, dir, maptype, wreqr, Cometd, metacardTemplate) {
        "use strict";

        var Metacard = {};

        ich.addTemplate('metacardTemplate', metacardTemplate);

        Metacard.MetacardDetailView = Backbone.View.extend({
            className : 'slide-animate',
            events: {
                'click .location-link': 'viewLocation',
                'click .nav-tabs' : 'onTabClick',
                'click #prevRecord' : 'previousRecord',
                'click #nextRecord' : 'nextRecord'
            },
            modelEvents: {
                'change': 'render'
            },
            initialize: function () {

                if(this.model.get('hash')) {
                    this.hash = this.model.get('hash');
                }

                var metacardResult = this.model.get("metacardResult").at(0);
                var searchResult = metacardResult.get("searchResult");
                var collection = searchResult.get("results");
                var index = collection.indexOf(metacardResult);

                if (index !== 0) {
                    this.prevModel = collection.at(index - 1);
                }
                if (index < collection.length - 1) {
                    this.nextModel = collection.at(index + 1);
                }
            },
            render: function () {
                var jsonObj = this.model.toJSON();
                jsonObj.mapAvailable = maptype.isMap();
                jsonObj.url = this.model.url;
                jsonObj.clientId = Cometd.Comet.getClientId();  //required for retrieve product notifications subscription
                this.$el.html(ich.metacardTemplate(jsonObj));

                if (_.isUndefined(this.prevModel)) {
                    $('#prevRecord', this.$el).addClass('disabled');
                }
                if (_.isUndefined(this.nextModel)) {
                    $('#nextRecord', this.$el).addClass('disabled');
                }

                return this;
            },
            updateScrollbar: function () {
                var view = this;
                // defer seems to be necessary for this to update correctly
                _.defer(function () {
                    view.$el.perfectScrollbar('update');
                });
            },
            onTabClick : function(e){
                this.updateScrollbar();
                this.hash = e.target.hash;
            },
            viewLocation: function () {
                wreqr.vent.trigger('search:mapshow', this.model);
            },
            previousRecord: function () {
                if (this.prevModel) {
                    this.prevModel.get("metacard").set('hash', this.hash);
                    this.model.set('context', false);
                    this.prevModel.get("metacard").set('direction', dir.downward);
                    this.prevModel.get("metacard").set('context', true);
                }
            },
            nextRecord: function () {
                if (this.nextModel) {
                    this.nextModel.get("metacard").set('hash', this.hash);
                    this.model.set('context', false);
                    this.nextModel.get("metacard").set('direction', dir.upward);
                    this.nextModel.get("metacard").set('context', true);
                }
            },
            close: function () {
                this.remove();
                this.stopListening();
                this.unbind();
            }
        });

        return Metacard;

});