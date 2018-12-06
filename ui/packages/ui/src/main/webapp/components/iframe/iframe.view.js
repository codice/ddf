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
/* global define */
import React from 'react'
import AdminLogViewer from 'logviewer-ui/src/main/webapp/js/main'
define(['marionette', './iframe.hbs', 'js/CustomElements'], function(
  Marionette,
  template,
  CustomElements
) {
  return Marionette.ItemView.extend({
    template({ url }) {
      let Component = () => (
        <iframe src={url} width="100%" scrolling="no">
          <p>Your browser does not support iframes.</p>
        </iframe>
      )
      switch (url) {
        case './logviewer/index.html':
          Component = AdminLogViewer
          break
      }
      return <Component />
    },
    tagName: CustomElements.register('iframe'),
    className: 'iframe-view',
  })
})
