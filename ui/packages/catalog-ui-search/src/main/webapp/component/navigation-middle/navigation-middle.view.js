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

const Marionette = require('marionette');
const CustomElements = require('../../js/CustomElements.js');
import * as React from 'react'
import { FormattedMessage } from 'react-intl'

module.exports = Marionette.LayoutView.extend({
  getLabel() {
    const { i18n, text } = this.options

    if (i18n) {
      const { id, defaultMessage } = this.options
      return <FormattedMessage id={id} defaultMessage={defaultMessage} />
    }

    return text
  },
  template(props) {
    return <div className={props.menuClass}>{this.getLabel()}</div>
  },
  tagName: CustomElements.register('navigation-middle'),
  serializeData: function() {
    return {
      menuClass: this.options.classes,
    }
  },
})
