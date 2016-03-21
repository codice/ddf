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
    'direction',
    'maptype',
    'wreqr',
    'cometdinit',
    'text!templates/metacard.handlebars',
    'js/view/Modal',
    'text!templates/metacardActionModal.handlebars',
    'js/view/NearbyLocation.view',
    'js/model/NearbyLocation',
    'js/store'
], function ($, _, Marionette, dir, maptype, wreqr, Cometd, metacardTemplate, Modal, metacardActionTemplate, NearbyLocationView, NearbyLocation, store) {
    'use strict';
    var Metacard = {};
    var nearbyChecked = false;
    Metacard.ActionModal = Modal.extend({
        template: metacardActionTemplate,
        initialize: function () {
            // there is no automatic chaining of initialize.
            Modal.prototype.initialize.apply(this, arguments);
        }
    });
    Metacard.MetacardDetailView = Marionette.LayoutView.extend({
        className: 'slide-animate height-full',
        template: metacardTemplate,
        regions: { nearby: '#nearby' },
        events: {
            'click .location-link': 'viewLocation',
            'click .nav-tabs': 'onTabClick',
            'click #prevRecord': 'previousRecord',
            'click #nextRecord': 'nextRecord',
            'click .metacard-action-link': 'metacardActionModal'
        },
        attributes: { 'allowScroll': false },
        modelEvents: { 'change': 'render' },
        initialize: function () {
            if (this.model.get('hash')) {
                this.hash = this.model.get('hash');
            }
            var collection = this.model.collection;
            var index = collection.indexOf(this.model.parents[0]);
            if (index !== 0) {
                this.prevModel = collection.at(index - 1);
            }
            if (index < collection.length - 1) {
                this.nextModel = collection.at(index + 1);
            }
            this.listenTo(wreqr.vent, 'search:beginMerge', this.invalidateList);
            this.types = store.get('sources').types();
        },
        onRender: function () {
            this.updateIterationControls();
            var view = this;
            _.defer(function () {
                view.$('.tab-content').perfectScrollbar({ suppressScrollX: true });
            });
            if (this.model.get('geometry')) {
                var nearby = new NearbyLocation({ geo: this.model.get('geometry') });
                this.showChildView('nearby', new NearbyLocationView({ model: nearby }));
            }
        },
        serializeData: function () {
            var type;
            if (this.types) {
                var typeObj = this.types.findWhere({ value: this.model.get('properties').get('metadata-content-type') });
                if (typeObj) {
                    type = typeObj.get('name');
                }
            }
            return _.extend(this.model.toJSON(), {
                mapAvailable: maptype.isMap(),
                url: this.model.url,
                clientId: Cometd.Comet.getClientId(),
                mappedType: type
            });
        },
        updateIterationControls: function () {
            if (_.isUndefined(this.prevModel)) {
                $('#prevRecord', this.$el).addClass('disabled');
            }
            if (_.isUndefined(this.nextModel)) {
                $('#nextRecord', this.$el).addClass('disabled');
            }
        },
        updateScrollbar: function () {
            this.$('.tab-content').perfectScrollbar({ suppressScrollX: true });
            var view = this;
            // defer seems to be necessary for this to update correctly
            _.defer(function () {
                view.$el.perfectScrollbar('update');
            });
        },
        onTabClick: function (e) {
            this.updateScrollbar();
            this.hash = e.target.hash;
        },
        viewLocation: function () {
            wreqr.vent.trigger('search:mapshow', this.model);
        },
        invalidateList: function () {
            delete this.prevModel;
            delete this.nextModel;
            this.updateIterationControls();
        },
        previousRecord: function () {
            if (this.prevModel) {
                this.prevModel.get('metacard').set({ hash: this.hash });
                wreqr.vent.trigger('metacard:selected', dir.downward, this.prevModel.get('metacard'));
                nearbyChecked = false;
            }
        },
        nextRecord: function () {
            if (this.nextModel) {
                this.nextModel.get('metacard').set({ hash: this.hash });
                wreqr.vent.trigger('metacard:selected', dir.upward, this.nextModel.get('metacard'));
                nearbyChecked = false;
            }
        },
        metacardActionModal: function (e) {
            var index = e.target.hash.replace('#', '');
            var action = this.model.get('actions').at(index);
            var modal = new Metacard.ActionModal({ model: action });
            wreqr.vent.trigger('showModal', modal);
        }
    });
    return Metacard;
});
