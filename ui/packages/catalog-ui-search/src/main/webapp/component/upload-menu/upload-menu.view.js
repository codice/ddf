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
/*global define*/
define([
  'wreqr',
  'marionette',
  'underscore',
  'jquery',
  './upload-menu.hbs',
  'js/CustomElements',
  'js/store',
  'component/upload/upload',
  'js/Common',
], function(
  wreqr,
  Marionette,
  _,
  $,
  template,
  CustomElements,
  store,
  uploadInstance,
  Common
) {
  return Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('upload-menu'),
    onFirstRender: function() {
      this.listenTo(uploadInstance, 'change:currentUpload', this.render)
    },
    serializeData: function() {
      if (uploadInstance.get('currentUpload') === undefined) {
        return {}
      }
      return {
        when: Common.getMomentDate(
          uploadInstance.get('currentUpload').get('sentAt')
        ),
      }
    },
  })
})
