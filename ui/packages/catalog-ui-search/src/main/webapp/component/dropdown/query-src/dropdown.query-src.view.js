const DropdownView = require('../dropdown.view')
const ComponentView = require('../../query-src/query-src.view.js')
const sources = require('../../singletons/sources-instance.js')
const properties = require('../../../js/properties.js')
import * as React from 'react'
import { FormattedMessage } from 'react-intl'

const renderSourceLocal = source => (
  <React.Fragment>
    {source.local ? (
      <i class="fa source-icon fa-home" />
    ) : (
      <i class="fa source-icon fa-cloud" />
    )}
  </React.Fragment>
)

const renderSourceAvailable = source => (
  <React.Fragment>
    <span className={'text-src ' + (source.available ? 'is-available' : '')}>
      {!source.available ? (
        <i class="fa fa-exclamation-triangle src-availability" />
      ) : null}
      {renderSourceLocal(source)}
      <span class="src-title">{source.id}</span>
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
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = this.model
    this.listenTo(this.model, 'change:federation', this.render)
  },
  isCentered: true,
  getCenteringElement: function() {
    return this.el.querySelector('.dropdown-container')
  },
  hasTail: true,
  hasLimitedWidth: true,
  serializeData: function() {
    const srcs = this.model.get('value')
    return {
      sources: sources.toJSON().filter(function(src) {
        return srcs.indexOf(src.id) !== -1
      }),
      enterprise: this.model.get('federation') === 'enterprise',
      localCatalog: sources.localCatalog,
      isLocalCatalogEnabled: !properties.isDisableLocalCatalog(),
    }
  },
})
