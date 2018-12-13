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
const Marionette = require('marionette')
const React = require('react')
const template = require('./map-settings.hbs')

import SettingsView from '../../react-component/container/map-settings/map-settings';
// const CustomElements = require('../../js/CustomElements.js')
// const Property = require('../property/property.js')
// const PropertyView = require('../property/property.view.js')
// const user = require('../singletons/user-instance.js')
// const mtgeo = require('mt-geo')
// const Common = require('../../js/Common.js')

// const exampleLat = '14.94'
// const exampleLon = '-11.875'
// const exampleDegrees = mtgeo.toLat(exampleLat) + ' ' + mtgeo.toLon(exampleLon)
// const exampleDecimal = exampleLat + ' ' + exampleLon
// const exampleMgrs = '4Q FL 23009 12331'
// const exampleUtm = '14 1925mE 1513mN'

// function getExample(formatValue) {
//   switch (formatValue) {
//     case 'degrees':
//       return exampleDegrees
//     case 'decimal':
//       return exampleDecimal
//     case 'mgrs':
//       return exampleMgrs
//     case 'utm':
//       return exampleUtm
//   }
//   throw 'Unrecognized coordinate format value [' + formatValue + ']'
// }

module.exports = Marionette.LayoutView.extend({
  template() {
    return (
      <SettingsView coordinateFormat="mgrs" coordinateFormatExample="14.94 -11.875" />
    )
  }
  // template: template,
  // tagName: CustomElements.register('map-settings'),
  // modelEvents: {},
  // events: {},
  // regions: {
  //   coordinateFormat: '> .property-coordinate-format',
  //   coordinateFormatExample: '> .property-coordinate-example',
  // },
  // ui: {},
  // initialize: function() {
  //   this.listenTo(
  //     user.get('user').get('preferences'),
  //     'change:coordinateFormat',
  //     this.onBeforeShow
  //   )
  // },
  // onBeforeShow: function() {
  //   this.setupResultCount()
  //   this.setupCoordinateExample()
  // },
  // setupCoordinateExample: function() {
  //   var coordinateFormat = user
  //     .get('user')
  //     .get('preferences')
  //     .get('coordinateFormat')

  //   this.coordinateFormatExample.show(
  //     new PropertyView({
  //       model: new Property({
  //         label: 'Example Coordinates',
  //         value: [getExample(coordinateFormat)],
  //         type: 'STRING',
  //       }),
  //     })
  //   )
  // },
  // setupResultCount: function() {
  //   var coordinateFormat = user
  //     .get('user')
  //     .get('preferences')
  //     .get('coordinateFormat')

  //   this.coordinateFormat.show(
  //     new PropertyView({
  //       model: new Property({
  //         label: 'Coordinate Format',
  //         value: [coordinateFormat],
  //         enum: [
  //           {
  //             label: 'Degrees, Minutes, Seconds',
  //             value: 'degrees',
  //           },
  //           {
  //             label: 'Decimal',
  //             value: 'decimal',
  //           },
  //           {
  //             label: 'MGRS',
  //             value: 'mgrs',
  //           },
  //           {
  //             label: 'UTM/UPS',
  //             value: 'utm',
  //           },
  //         ],
  //       }),
  //     })
  //   )

  //   this.coordinateFormat.currentView.turnOnEditing()
  //   this.listenTo(
  //     this.coordinateFormat.currentView.model,
  //     'change:value',
  //     this.save
  //   )
  // },
  // save: function() {
  //   Common.queueExecution(() => {
  //     var preferences = user.get('user').get('preferences')
  //     preferences.set({
  //       coordinateFormat: this.coordinateFormat.currentView.model.getValue()[0],
  //     })
  //     preferences.savePreferences()
  //   })
  // },
  // repositionDropdown: function() {
  //   this.$el.trigger('repositionDropdown.' + CustomElements.getNamespace())
  // },
})
