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

const Marionette = require('marionette')
const IngestView = require('../ingest/ingest.view')
const BuilderView = require('../builder/builder.view')
const template = require('./newitem.hbs')
const CustomElements = require('../../js/CustomElements')



module.exports = Marionette.LayoutView.extend({
    template,
    regions: {
        upload: '.upload-menu',
        manual: '.manual-menu'
    },
    tagName: CustomElements.register('newitem'),
    initialize(options) {
        this.getRegion('upload-menu').show(new IngestView());
        this.getRegion('manual-menu').show(new BuilderView());
    }
})
