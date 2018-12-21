/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
import * as React from 'react'
import MapSettingsPresentation from '../../presentation/map-settings'
import { hot } from 'react-hot-loader'
const Common = require('../../../js/Common.js')
const user = require('../../../component/singletons/user-instance.js')
const mtgeo = require('mt-geo')

const exampleLat = '14.94'
const exampleLon = '-11.875'
const exampleDegrees = mtgeo.toLat(exampleLat) + ' ' + mtgeo.toLon(exampleLon)
const exampleDecimal = exampleLat + ' ' + exampleLon
const exampleMgrs = '4Q FL 23009 12331'
const exampleUtm = '14 1925mE 1513mN'

const getExample = (formatValue: string) => {
  switch (formatValue) {
    case 'degrees':
      return exampleDegrees
    case 'decimal':
      return exampleDecimal
    case 'mgrs':
      return exampleMgrs
    case 'utm':
      return exampleUtm
  }
  throw 'Unrecognized coordinate format value [' + formatValue + ']'
}

const save = (newFormat: string) => {
  Common.queueExecution(() => {
    var preferences = user.get('user').get('preferences')
    preferences.set({
      coordinateFormat: newFormat,
    })
    preferences.savePreferences()
  })
}

type State = {
  selected: string
}

class MapSettings extends React.Component<{}, State> {
  constructor() {
    super({})
    this.state = {
      selected: user
        .get('user')
        .get('preferences')
        .get('coordinateFormat'),
    }
  }

  update(newFormat: string) {
    save(newFormat)
    this.setState({ selected: newFormat })
  }

  render() {
    const { selected } = this.state

    const mapSettingsProps = {
      selected,
      example: getExample(selected),
      update: (newFormat: string) => this.update(newFormat),
    }

    return <MapSettingsPresentation {...mapSettingsProps} />
  }
}

export default hot(module)(MapSettings)
