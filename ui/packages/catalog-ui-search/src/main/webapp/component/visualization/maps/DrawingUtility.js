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

//allows us to get around svg security restrictions in IE11 (see using svg in opengl)
//make our own image and manually set dimensions because of IE: https://github.com/openlayers/openlayers/issues/3939
const _ = require('underscore')

const defaultColor = '#3c6dd5'

module.exports = {
  getCircle(options) {
    _.defaults(options, {
      diameter: 22,
      fillColor: defaultColor,
      strokeWidth: 2,
      strokeColor: 'white',
    })
    const radius = options.diameter / 2
    const canvas = document.createElement('canvas')
    canvas.width = options.diameter
    canvas.height = options.diameter
    const ctx = canvas.getContext('2d')
    ctx.beginPath()
    ctx.strokeStyle = options.strokeColor
    ctx.lineWidth = options.strokeWidth
    ctx.fillStyle = options.fillColor
    ctx.arc(
      radius,
      radius,
      radius - options.strokeWidth / 2,
      0,
      2 * Math.PI,
      false
    )
    ctx.fill()
    ctx.stroke()
    return canvas
  },
  getCircleWithText(options) {
    _.defaults(options, {
      diameter: 44,
      fillColor: defaultColor,
      strokeWidth: 2,
      strokeColor: 'white',
      text: '',
      textColor: 'white',
    })
    const canvas = this.getCircle(options)
    const ctx = canvas.getContext('2d')
    ctx.font = '16pt Helvetica'
    ctx.fillStyle = options.textColor
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(options.text, options.diameter / 2, options.diameter / 2)
    return canvas
  },
  getCircleWithIcon(options) {
    _.defaults(options, {
      diameter: 24,
      fillColor: defaultColor,
      strokeWidth: 2,
      strokeColor: 'white',
      text: '',
      textColor: 'white',
    })
    const canvas = this.getCircle(options)
    const ctx = canvas.getContext('2d')
    const style = options.icon.style

    ctx.font = style.size + ' ' + style.font
    ctx.fillStyle = options.textColor
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'

    if (style.code) {
      let icon = String.fromCharCode(parseInt(style.code, 16))
      ctx.fillText(icon, options.diameter / 2, options.diameter / 2)
    }
    return canvas
  },
  getPin(options) {
    _.defaults(options, {
      width: 39,
      height: 40,
      fillColor: defaultColor,
      strokeWidth: 2,
      strokeColor: 'white',
      textColor: 'white',
    })
    const canvas = document.createElement('canvas')
    canvas.width = options.width
    canvas.height = options.height
    const ctx = canvas.getContext('2d')

    ctx.strokeStyle = options.strokeColor
    ctx.lineWidth = options.strokeWidth
    ctx.fillStyle = options.fillColor

    const s = options.scale
    ctx.beginPath()
    ctx.moveTo(19.36, 2)
    ctx.bezierCurveTo(11.52, 2, 4.96, 6.64, 4.96, 14.64)
    ctx.bezierCurveTo(4.96, 17.92, 6.08, 20.96, 7.84, 23.44)
    ctx.lineTo(19.52, 38.96)
    ctx.lineTo(31.2, 23.44)
    ctx.bezierCurveTo(33.04, 20.96, 34.08, 17.92, 34.08, 14.64)
    ctx.bezierCurveTo(34.08, 6.64, 27.6, 2, 19.52, 2)
    ctx.fillStyle = options.fillColor
    ctx.fill()
    ctx.stroke()

    const style = options.icon.style
    if (style.code) {
      ctx.font = style.size + ' ' + style.font
      ctx.fillStyle = options.textColor
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'

      let icon = String.fromCharCode(parseInt(style.code, 16))
      ctx.fillText(icon, options.width / 2, options.height / 2 - 5)
    }

    return canvas
  },
}
