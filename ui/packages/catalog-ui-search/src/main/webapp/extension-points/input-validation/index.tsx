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
const InputUtil = require('../../component/input/InputUtil.js')

const isValid = (model: any) => {
  const value = InputUtil.getValue(model)
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

export default isValid
