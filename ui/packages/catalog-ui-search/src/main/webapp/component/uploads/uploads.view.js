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
/*global define, setTimeout*/
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./uploads.hbs')
const CustomElements = require('../../js/CustomElements.js')
const UploadBatchItemCollectionView = require('../upload-batch-item/upload-batch-item.collection.view.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('uploads'),
  modelEvents: {},
  events: {},
  regions: {
    uploadList: '.uploads-list',
  },
  ui: {},
  onBeforeShow: function() {
    this.uploadList.show(new UploadBatchItemCollectionView())
  },
})
