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
import lightBaseTheme from 'material-ui/styles/baseThemes/lightBaseTheme'
import { fromJS } from 'immutable'

export default fromJS(lightBaseTheme)
  .mergeDeep({
    textField: {
      errorColor: '#E74C3C',
    },
    raisedButton: {
      disabledColor: '#DDDDDD',
    },
    tableRow: {
      selectedColor: '#DDDDDD',
    },
    palette: {
      textColor: '#777777',
      primary1Color: '#18BC9C',
      accent1Color: '#2C3E50',
      accent2Color: '#FFFFFF',
      backdropColor: '#ECF0F1',
      errorColor: '#E74C3C',
      warningColor: '#DC8201',
      successColor: '#18BC9C',
      canvasColor: '#FFFFFF',
      disabledColor: '#DDDDDD',
    },
  })
  .toJS()
