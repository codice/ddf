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
