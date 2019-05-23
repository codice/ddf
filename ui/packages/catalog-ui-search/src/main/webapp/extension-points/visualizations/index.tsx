const HistogramView = require('../../component/visualization/histogram/histogram.view.js')
const TableView = require('../../component/visualization/table/table-viz.view.js')
const InspectorView = require('../../component/visualization/inspector/inspector.view.js')
const LowBandwidthMapView = require('../../component/visualization/low-bandwidth-map/low-bandwidth-map.view.js')

export default [
  {
    id: 'openlayers',
    title: '2D Map',
    view: LowBandwidthMapView,
    icon: 'fa fa-map',
    options: {
      desiredContainer: 'openlayers',
    },
  },
  {
    id: 'cesium',
    title: '3D Map',
    view: LowBandwidthMapView,
    icon: 'fa fa-globe',
    options: {
      desiredContainer: 'cesium',
    },
  },
  {
    id: 'histogram',
    title: 'Histogram',
    icon: 'fa fa-bar-chart',
    view: HistogramView,
  },
  {
    id: 'table',
    title: 'Table',
    icon: 'fa fa-table',
    view: TableView,
  },
  {
    id: 'inspector',
    title: 'Inspector',
    icon: 'fa fa-info',
    view: InspectorView,
  },
]
