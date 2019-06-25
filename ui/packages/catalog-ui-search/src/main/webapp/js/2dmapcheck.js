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

const _ = require('underscore')

module.exports = {
  is2dAvailable: undefined,
  isAvailable() {
    if (_.isUndefined(this.is2dAvailable)) {
      this.is2dAvailable = false

      try {
        const canvas = document.createElement('canvas')
        const twoD = canvas.getContext('2d')
        if (twoD) {
          this.is2dAvailable = true
        }
      } catch (e) {
        // canvas not supported by browser
      }
    }
    return this.is2dAvailable
  },
}
