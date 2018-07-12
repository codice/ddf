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

const bridge = require('./marionette-bridge');
const LocationInput = bridge(require('./location'));

if (process.env.NODE_ENV !== 'production') {
    module.hot.accept('./location', () => {
        LocationInput.reload(require('./location'));
    });
}

const Marionette = require('marionette');
const _ = require('underscore');
const wreqr = require('wreqr');
const store = require('js/store');
const CustomElements = require('js/CustomElements');
const LocationNewModel = require('./location-new');
const CQLUtils = require('js/CQLUtils');
const ShapeUtils = require('js/ShapeUtils');

const minimumDifference = 0.0001;
const minimumBuffer = 0.000001;

module.exports = Marionette.LayoutView.extend({
    template: () => `<div class="location-input"></div>`,
    tagName: CustomElements.register('location-old'),
    regions: {
        location: '.location-input'
    },
    initialize: function(options) {
        this.propertyModel = this.model;
        this.model = new LocationNewModel();
        _.bindAll.apply(_, [this].concat(_.functions(this))); // underscore bindAll does not take array arg
    },
    onRender: function() {
        this.location.show(
            new LocationInput({
                model: this.model,
                onDraw: (drawingType) => {
                    wreqr.vent.trigger('search:draw' + this.model.get('mode'), this.model);
                }
            })
        );
    },
    getCurrentValue: function() {
        return this.model.getValue();
    },
    onDestroy: function() {
        wreqr.vent.trigger('search:drawend', this.model);
    }
});
