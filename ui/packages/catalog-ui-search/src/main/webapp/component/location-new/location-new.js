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
const _ = require('underscore');
const Backbone = require('backbone');
const { validateWkt, validateDd, validateDms, validateUsng, ddToWkt, dmsToWkt, usngToWkt } = require('./utils');
const { ddModel, dmsModel, usngModel } = require('./models');

module.exports = Backbone.AssociatedModel.extend({
    defaults: {
        showErrors: true,
        valid: true,
        error: undefined,
        mode: 'wkt',
        wkt: '',
        dd: ddModel,
        dms: dmsModel,
        usng: usngModel
    },

    initialize: function() {
        this.listenTo(this, 'change:wkt change:dms change:dd change:usng change:mode', this.validate.bind(this));
    },

    isValid: function() {
        return this.get('valid');
    },

    /*
     * Return the active input converted to WKT. If the input failed validation, return "INVALID".
     * If the input is blank, return null.
     */
    getValue: function() {
        if (!this.isValid()) {
            return "INVALID";
        }

        const mode = this.get('mode');
        var wkt;
        switch (mode) {
            case 'wkt':
                wkt = this.get(mode);
                break;
            case 'dd':
                wkt = ddToWkt(this.get(mode));
                break;
            case 'dms':
                wkt = dmsToWkt(this.get(mode));
                break;
            case 'usng':
                wkt = usngToWkt(this.get(mode));
                break;
        }
        return wkt;
    },

    /* Run the appropriate validator for the active mode. Blank input is considered valid */
    validate: function() {
        const mode = this.get('mode');
        var validationReport;
        switch (mode) {
            case 'wkt':
                validationReport = validateWkt(this.get(mode));
                break;
            case 'dd':
                validationReport = validateDd(this.get(mode));
                break;
            case 'dms':
                validationReport = validateDms(this.get(mode));
                break;
            case 'usng':
                validationReport = validateUsng(this.get(mode));
                break;
        }
        this.set('valid', validationReport ? validationReport.valid : true);
        this.set('error', validationReport ? validationReport.error : false);
    },
});