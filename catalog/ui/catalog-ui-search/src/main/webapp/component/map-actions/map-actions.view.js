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
    'wreqr',
    'text!./map-actions.hbs'
], function (_, Marionette, wreqr, template) {
    "use strict";

    var mapActionsView = Marionette.ItemView.extend({
        template: template,

        events: {
            'click .overlay-link': 'overlayImage'
        },

        serializeData: function () {
            return _.extend(this.model.toJSON(), {
                mapActions: this.getMapActions(),
                overlayActions: this.getOverlayActions()
            });
        },

        getActions: function () {
            return new Backbone.Collection(this.model.get('actions'));
        },

        getMapActions: function () {
            return this.getActions().filter(function(action) {
                return action.get('id').startsWith('catalog.data.metacard.map.');
            });
        },

        getOverlayActions: function () {
            var modelOverlayActions = this.getActions().filter(function(action) {
                return action.get('id').startsWith('catalog.data.metacard.map.overlay.');
            });

            var _this = this;
            return _.map(modelOverlayActions, function(modelOverlayAction) {
                return {
                    description: modelOverlayAction.get('description'),
                    url: modelOverlayAction.get('url'),
                    overlayText: _this.getOverlayText(modelOverlayAction.get('url'))
                };
            });
        },

        getOverlayText: function (actionUrl) {
            var overlayTransformerPrefix = 'overlay.';
            var overlayTransformerIndex = actionUrl.lastIndexOf(overlayTransformerPrefix);
            if (overlayTransformerIndex >= 0) {
                var overlayName = actionUrl.substr(overlayTransformerIndex + overlayTransformerPrefix.length);
                return "Overlay " + overlayName + " on the map";
            }

            return "";
        },

        overlayImage: function (event) {
            var clickedOverlayUrl = event.target.getAttribute('data-url');
            var currentOverlayUrl = this.model.get('currentOverlayUrl');

            var removeOverlay = clickedOverlayUrl === currentOverlayUrl;

            if (removeOverlay) {
                this.model.unset('currentOverlayUrl', {silent: true});
                wreqr.vent.trigger('metacard:overlay:remove', this.model.get('metacard').get('id'));
            } else {
                this.model.set('currentOverlayUrl', clickedOverlayUrl, {silent: true});
                this.model.get('metacard').set('currentOverlayUrl', clickedOverlayUrl);
                wreqr.vent.trigger('metacard:overlay', this.model.get('metacard'));
            }
            this.render();
        }
    });

    return mapActionsView;
});