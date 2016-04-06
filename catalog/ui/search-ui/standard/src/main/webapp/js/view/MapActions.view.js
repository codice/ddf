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
        'icanhaz',
        'wreqr',
        'text!templates/mapActions.handlebars'
    ],
    function (_, Marionette, ich, wreqr, mapActionsTemplate) {
        "use strict";

        ich.addTemplate('mapActionsTemplate', mapActionsTemplate);

        var mapActionsView = Marionette.ItemView.extend({
            template: 'mapActionsTemplate',

            events: {
                'click .overlay-link': 'overlayImage'
            },

            serializeData: function () {
                return _.extend(this.model.toJSON(), {
                    mapActions: this.getMapActions(),
                    overlayActions: this.getOverlayActions()
                });
            },

            getMapActions: function () {
                if (this.model.has('actions')) {
                    return this.model.get('actions').filter(function(action) {
                        return action.get('id').startsWith('catalog.data.metacard.map.');
                    });
                }

                return [];
            },

            getOverlayActions: function () {
                if (this.model.has('actions')) {
                    var modelOverlayActions = this.model.get('actions').filter(function(action) {
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
                }

                return [];
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
                    wreqr.vent.trigger('metacard:overlay:remove', this.model.get('properties').get('id'));
                } else {
                    this.model.set('currentOverlayUrl', clickedOverlayUrl, {silent: true});
                    wreqr.vent.trigger('metacard:overlay', this.model);
                }
                this.render();
            }
        });

        return mapActionsView;
});
