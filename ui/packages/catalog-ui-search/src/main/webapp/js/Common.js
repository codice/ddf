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

/*jshint bitwise: false*/
const $ = require('jquery')
const moment = require('moment')
const _ = require('underscore')
require('./requestAnimationFramePolyfill')

const timeZones = {
  UTC: 'Etc/UTC',
  '-12': 'Etc/GMT+12',
  '-11': 'Etc/GMT+11',
  '-10': 'Etc/GMT+10',
  '-9': 'Etc/GMT+9',
  '-8': 'Etc/GMT+8',
  '-7': 'Etc/GMT+7',
  '-6': 'Etc/GMT+6',
  '-5': 'Etc/GMT+5',
  '-4': 'Etc/GMT+4',
  '-3': 'Etc/GMT+3',
  '-2': 'Etc/GMT+2',
  '-1': 'Etc/GMT+1',
  '1': 'Etc/GMT-1',
  '2': 'Etc/GMT-2',
  '3': 'Etc/GMT-3',
  '4': 'Etc/GMT-4',
  '5': 'Etc/GMT-5',
  '6': 'Etc/GMT-6',
  '7': 'Etc/GMT-7',
  '8': 'Etc/GMT-8',
  '9': 'Etc/GMT-9',
  '10': 'Etc/GMT-10',
  '11': 'Etc/GMT-11',
  '12': 'Etc/GMT-12',
}

const dateTimeFormats = {
  ISO: { datetimefmt: 'YYYY-MM-DD[T]HH:mm:ss.SSSZ', timefmt: 'HH:mm:ssZ' },
  24: { datetimefmt: 'DD MMM YYYY HH:mm:ss.SSS Z', timefmt: 'HH:mm:ss Z' },
  12: { datetimefmt: 'DD MMM YYYY h:mm:ss.SSS a Z', timefmt: 'h:mm:ss a Z' },
}

module.exports = {
  //randomly generated guid guaranteed to be unique ;)
  undefined: '2686dcb5-7578-4957-974d-aaa9289cd2f0',
  coreTransitionTime: 250,
  generateUUID(properties = require('properties')) {
    let d = new Date().getTime()
    if (window.performance && typeof window.performance.now === 'function') {
      d += performance.now() //use high-precision timer if available
    }
    const uuid = 'xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx'.replace(/[xy]/g, c => {
      const r = (d + Math.random() * 16) % 16 | 0
      d = Math.floor(d / 16)
      return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16)
    })
    if (!properties.useHyphensInUuid) return uuid

    const chunks = uuid.match(/.{1,4}/g)

    const prefix = chunks.slice(0, 2).join('')
    const middle = chunks.slice(2, 5).join('-')
    const suffix = chunks.slice(5, chunks.length).join('')

    return `${prefix}-${middle}-${suffix}`
  },
  cqlToHumanReadable(cql) {
    if (cql === undefined) {
      return cql
    }
    cql = cql.replace(new RegExp('anyText ILIKE ', 'g'), '~')
    cql = cql.replace(new RegExp('anyText LIKE ', 'g'), '')
    cql = cql.replace(new RegExp('AFTER', 'g'), '>')
    cql = cql.replace(new RegExp('DURING', 'g'), 'BETWEEN')
    return cql
  },
  setupPopOver($component) {
    $component.find('[title]').each(function() {
      const $element = $(this)
      $element.popover({
        delay: {
          show: 1000,
          hide: 0,
        },
        trigger: 'hover',
      })
    })
  },
  getFileSize(item) {
    if (_.isUndefined(item)) {
      return 'Unknown Size'
    }
    const givenProductSize = item.replace(/[,]+/g, '').trim()
    //remove any commas and trailing whitespace
    const bytes = parseInt(givenProductSize, 10)
    const noUnitsGiven = /[0-9]$/
    //number without a word following
    const reformattedProductSize = givenProductSize.replace(/\s\s+/g, ' ')
    //remove extra whitespaces
    const finalFormatProductSize = reformattedProductSize.replace(
      /([0-9])([a-zA-Z])/g,
      '$1 $2'
    )
    //make sure there is exactly one space between number and unit
    const sizeArray = finalFormatProductSize.split(' ')
    //splits size into number and unit
    if (isNaN(bytes)) {
      return 'Unknown Size'
    }
    if (noUnitsGiven.test(givenProductSize)) {
      //need to parse number given and add units, number is assumed to be bytes
      let size,
        index,
        type = ['bytes', 'KB', 'MB', 'GB', 'TB']
      if (bytes === 0) {
        return '0 bytes'
      } else {
        index = Math.floor(Math.log(bytes) / Math.log(1024))
        if (index > 4) {
          index = 4
        }
        size = (bytes / Math.pow(1024, index)).toFixed(index < 2 ? 0 : 1)
      }
      return size + ' ' + type[index]
    } else {
      //units were included with size
      switch (sizeArray[1].toLowerCase()) {
        case 'bytes':
          return sizeArray[0] + ' bytes'
        case 'b':
          return sizeArray[0] + ' bytes'
        case 'kb':
          return sizeArray[0] + ' KB'
        case 'kilobytes':
          return sizeArray[0] + ' KB'
        case 'kbytes':
          return sizeArray[0] + ' KB'
        case 'mb':
          return sizeArray[0] + ' MB'
        case 'megabytes':
          return sizeArray[0] + ' MB'
        case 'mbytes':
          return sizeArray[0] + ' MB'
        case 'gb':
          return sizeArray[0] + ' GB'
        case 'gigabytes':
          return sizeArray[0] + ' GB'
        case 'gbytes':
          return sizeArray[0] + ' GB'
        case 'tb':
          return sizeArray[0] + ' TB'
        case 'terabytes':
          return sizeArray[0] + ' TB'
        case 'tbytes':
          return sizeArray[0] + ' TB'
        default:
          return 'Unknown Size'
      }
    }
  },
  getFileSizeGuaranteedInt(item) {
    if (_.isUndefined(item)) {
      return 'Unknown Size'
    }
    const bytes = parseInt(item, 10)
    if (isNaN(bytes)) {
      return item
    }
    let size,
      index,
      type = ['bytes', 'KB', 'MB', 'GB', 'TB']
    if (bytes === 0) {
      return '0 bytes'
    } else {
      index = Math.floor(Math.log(bytes) / Math.log(1024))
      if (index > 4) {
        index = 4
      }
      size = (bytes / Math.pow(1024, index)).toFixed(index < 2 ? 0 : 1)
    }
    return size + ' ' + type[index]
  },
  //can be deleted once histogram changes are merged
  getHumanReadableDateTime(date) {
    return moment(date).format(dateTimeFormats['24']['datetimefmt'])
  },
  getDateTimeFormats() {
    return dateTimeFormats
  },
  getTimeZones() {
    return timeZones
  },
  getMomentDate(date) {
    return moment(date).fromNow()
  },
  getImageSrc(img) {
    if (
      typeof img === 'string' &&
      (img === '' || img.substring(0, 4) === 'http')
    )
      return img

    return 'data:image/png;base64,' + img
  },
  getResourceUrlFromThumbUrl(url) {
    return url.replace(/=thumbnail[_=&\d\w\s;]+/, '=resource')
  },
  cancelRepaintForTimeframe(requestDetails) {
    if (requestDetails) {
      window.cancelAnimationFrame(requestDetails.requestId)
    }
  },
  repaintForTimeframe(time, callback) {
    const requestDetails = {
      requestId: undefined,
    }
    const timeEnd = Date.now() + time
    const repaint = function() {
      callback()
      if (Date.now() < timeEnd) {
        requestDetails.requestId = window.requestAnimationFrame(() => {
          repaint()
        })
      }
    }
    requestDetails.requestId = window.requestAnimationFrame(() => {
      repaint()
    })
    return requestDetails
  },
  executeAfterRepaint(callback) {
    return window.requestAnimationFrame(() => {
      window.requestAnimationFrame(callback)
    })
  },
  queueExecution(callback) {
    return setTimeout(callback, 0)
  },
  escapeHTML(value) {
    return $('<div>')
      .text(value)
      .html()
  },
  duplicate(reference) {
    return JSON.parse(JSON.stringify(reference))
  },
  safeCallback(callback) {
    return function() {
      if (!this.isDestroyed) {
        callback.apply(this, arguments)
      }
    }
  },
  wrapMapCoordinates(x, [min, max]) {
    const d = max - min
    return ((((x - min) % d) + d) % d) + min
  },
  wrapMapCoordinatesArray(coordinates) {
    return coordinates.map(([lon, lat]) => [
      this.wrapMapCoordinates(lon, [-180, 180]),
      this.wrapMapCoordinates(lat, [-90, 90]),
    ])
  },
}
