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

const DropdownView = require('../dropdown.view')
const ComponentView = require('../../query-src/query-src.view.js')
const sources = require('../../singletons/sources-instance.js')
const properties = require('../../../js/properties.js')
import * as React from 'react'
import { FormattedMessage } from 'react-intl'

const renderSourceLocal = source => (
  <React.Fragment>
    {source.local ? (
      <i className="fa source-icon fa-home" />
    ) : (
      <i className="fa source-icon fa-cloud" />
    )}
  </React.Fragment>
)

const renderSourceAvailable = source => (
  <React.Fragment>
    <span className={'text-src ' + (source.available ? 'is-available' : '')}>
      {!source.available ? (
        <i className="fa fa-exclamation-triangle src-availability" />
      ) : null}
      {renderSourceLocal(source)}
      <span className="src-title">{source.id}</span>
    </span>
  </React.Fragment>
)

module.exports = DropdownView.extend({
  template(props) {
    return (
      <div>
        <div className="dropdown-label">
          <FormattedMessage id="sources.title" defaultMessage="Sources" />
        </div>
        <div className="dropdown-container">
          <div className="dropdown-text is-input">
            {props.enterprise ? (
              <span className="text-src is-available">
                <FormattedMessage
                  id="sources.options.all"
                  defaultMessage="All Sources"
                />
              </span>
            ) : (
              props.sources.map(source => renderSourceAvailable(source))
            )}
            {!props.sources && props.isLocalCatalogEnabled ? (
              <span className="text-src is-available">
                Local Only {props.localCatalog}
              </span>
            ) : null}
          </div>
          <span className="dropdown-icon fa fa-caret-down" />
        </div>
      </div>
    )
  },
  className: 'is-querySrc',
  componentToShow: ComponentView,
  initializeComponentModel() {
    //override if you need more functionality
    this.modelForComponent = this.model
    this.listenTo(this.model, 'change:federation', this.render)
  },
  isCentered: true,
  getCenteringElement() {
    return this.el.querySelector('.dropdown-container')
  },
  hasTail: true,
  hasLimitedWidth: true,
  serializeData() {
    const srcs = this.model.get('value')
    return {
      sources: sources.toJSON().filter(src => srcs.indexOf(src.id) !== -1),
      enterprise: this.model.get('federation') === 'enterprise',
      localCatalog: sources.localCatalog,
      isLocalCatalogEnabled: !properties.isDisableLocalCatalog(),
    }
  },
})
