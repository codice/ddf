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
const user = require('../../component/singletons/user-instance.js')

const isValid = (model: any) => {
  const value = getValue(model)
  const choice = model
    .get('property')
    .get('enum')
    .filter(
      (choice: any) =>
        value.filter(
          (subvalue: any) =>
            JSON.stringify(choice.value) === JSON.stringify(subvalue) ||
            JSON.stringify(choice) === JSON.stringify(subvalue)
        ).length > 0
    )
  return choice.length > 0
}

function getValue(model: any) {
  const multivalued = model.get('property').get('enumMulti')
  let value = model.get('value')
  if (value !== undefined && model.get('property').get('type') === 'DATE') {
    if (multivalued && value.map) {
      value = value.map((subvalue: string) =>
        user.getUserReadableDateTime(subvalue)
      )
    } else {
      value = user.getUserReadableDateTime(value)
    }
  }
  if (!multivalued) {
    value = [value]
  }
  return value
}

export default isValid
