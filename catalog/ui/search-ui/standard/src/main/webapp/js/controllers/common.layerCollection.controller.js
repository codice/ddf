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
/*jshint newcap: false, bitwise: false */

define([
    'marionette'
], function (Marionette) {
    "use strict";

    var Controller = Marionette.Object.extend({
        initialize: function (options) {
            this.collection = options.collection;
            this.layerForCid = {};

            /*
             don't listen to individual model index changes here, b/c
             index changes are batched in move().
             */
            this.listenTo(this.collection, 'change:alpha', this.setAlpha);
            this.listenTo(this.collection, 'change:show', this.setShow);
        },
        move: function (model) {
            /*
             moving a layer will move other layers, so re-index all models
             then move all layers in batch. This logic supports moving a
             model "anywhere" within the list of models.
             */
            var newIndex = model.get('index');
            var oldIndex = model.previous('index');

            if (newIndex < oldIndex) { // lower model, perhaps raise other models.
                this.collection.each(function (otherModel) {
                    if (otherModel.cid !== model.cid) {
                        var otherIndex = otherModel.get('index');
                        if (otherIndex < oldIndex && otherIndex >= newIndex) {
                            otherModel.set('index', otherIndex + 1);
                        }
                    }
                });
            }
            else if (newIndex > oldIndex) { // raise model, perhaps lower other models.
                this.collection.each(function (otherModel) {
                    if (otherModel.cid !== model.cid) {
                        var otherIndex = otherModel.get('index');
                        if (otherIndex > oldIndex && otherIndex <= newIndex) {
                            otherModel.set('index', otherIndex - 1);
                        }
                    }
                });
            }
            this.reIndexAll(); // reIndexAll() must be defined in subclass.
        },
        onBeforeDestroy: function () {
            this.collection = null;
            this.layerForCid = null;
        }
    });

    return Controller;

});
