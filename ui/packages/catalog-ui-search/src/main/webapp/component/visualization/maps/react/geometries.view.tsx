import * as React from 'react'
const Marionette = require('marionette')
import Geometries from './geometries'

export const GeometriesView = Marionette.ItemView.extend({
  template() {
    const { selectionInterface, map } = this.options

    return (
      <Geometries
        selectionInterface={selectionInterface}
        map={map}
        isClustering={this.isClustering}
      />
    )
  },
  initialize() {
    this.isClustering = false
    this.render()
  },
  toggleClustering() {
    this.isClustering = !this.isClustering
    this.render()
  },
})
