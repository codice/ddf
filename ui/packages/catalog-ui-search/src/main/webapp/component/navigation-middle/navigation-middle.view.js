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
/*global require*/
var Marionette = require('marionette')
var CustomElements = require('../../js/CustomElements.js')
const properties = require('properties')
import * as React from 'react'
import { IntlProvider, FormattedMessage } from 'react-intl'

module.exports = Marionette.LayoutView.extend({
  template(props) {
    return (
      <div className={props.menuClass}>
        <IntlProvider locale={navigator.language}>
          <FormattedMessage
            id="sourcesKeyword"
            defaultMessage={props.menuText}
          />
        </IntlProvider>
      </div>
    )
  },
  tagName: CustomElements.register('navigation-middle'),
  serializeData: function() {
    /**
     * In order to support different locales in the future, this manual approach for keyword replacement
     * will need to include some sort of contextual key so that this text can be internationalized.
     */
    const menuText = properties.i18n[this.options.text] || this.options.text

    return {
      menuClass: this.options.classes,
      menuText: menuText,
    }
  },
})
