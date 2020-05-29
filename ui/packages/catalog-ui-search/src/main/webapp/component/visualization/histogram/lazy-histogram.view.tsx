import * as React from 'react'

const Marionette = require('marionette')
import Histogram from './histogram'

const LazyHistogramView = Marionette.LayoutView.extend({
  className: 'customElement',
  template() {
    return (
      <React.Fragment>
        <Histogram selectionInterface={this.options.selectionInterface} />
      </React.Fragment>
    )
  },
})

export default LazyHistogramView
