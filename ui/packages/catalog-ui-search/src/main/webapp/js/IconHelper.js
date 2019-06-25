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
const _ = require('lodash')
const _get = require('lodash/get')
const _map = {
  default: {
    class: 'fa fa-file',
    style: {
      code: 'f15b',
      font: 'FontAwesome',
      size: '12px',
    },
  },
  interactive: {
    class: 'fa fa-gamepad',
    style: {
      code: 'f11b',
      font: 'FontAwesome',
      size: '12px',
    },
  },
  dataset: {
    class: 'fa fa-database',
    style: {
      code: 'f1c0',
      font: 'FontAwesome',
      size: '12px',
    },
  },
  video: {
    class: 'fa fa-video-camera',
    style: {
      code: 'f03d',
      font: 'FontAwesome',
      size: '12px',
    },
  },
  collection: {
    class: 'fa fa-folder-open',
    style: {
      code: 'f07c',
      font: 'FontAwesome',
      size: '12px',
    },
  },
  event: {
    class: 'fa fa-bolt',
    style: {
      code: 'f0e7',
      font: 'FontAwesome',
      size: '12px',
    },
  },
  service: {
    class: 'fa fa-globe',
    style: {
      code: 'f0ac',
      font: 'FontAwesome',
      size: '12px',
    },
  },
  software: {
    class: 'fa fa-terminal',
    style: {
      code: 'f120',
      font: 'FontAwesome',
      size: '12px',
    },
  },
  sound: {
    class: 'fa fa-music',
    style: {
      code: 'f001',
      font: 'FontAwesome',
      size: '12px',
    },
  },
  text: {
    class: 'fa fa-file-text',
    style: {
      code: 'f15c',
      font: 'FontAwesome',
      size: '12px',
    },
  },
  document: {
    class: 'fa fa-file',
    style: {
      code: 'f15b',
      font: 'FontAwesome',
      size: '12px',
    },
  },
  image: {
    class: 'fa fa-camera',
    style: {
      code: 'f030',
      font: 'FontAwesome',
      size: '12px',
    },
  },
  track: {
    class: 'fa fa-thumb-tack',
    style: {
      code: 'f08d',
      font: 'FontAwesome',
      size: '15px',
    },
  },
}

/* Maps top-level mime type category names to the closest icon. */
const _mimeMap = {
  application: _map.document,
  audio: _map.sound,
  example: _map.default,
  font: _map.document,
  image: _map.image,
  message: _map.document,
  model: _map.dataset,
  multipart: _map.collection,
  text: _map.text,
  video: _map.video,
}

/*  This is the default icon that will be used if a Metacard cannot be
        mapped to an icon. Set default attributes to empty strings for no icon. */
const _default = _map.default

/* Remove resource keyword from datatype and covert to lowercase. */
function _formatAttribute(attr) {
  if (attr !== undefined) {
    return attr.toLowerCase().replace(' resource', '')
  }
  return attr
}

/* Checks if the attribute value exists in the icon map. */
function _iconExistsInMap(attr, map) {
  if (attr instanceof Array) {
    attr = attr[0]
  }
  if (
    attr !== undefined &&
    attr.length > 0 &&
    map.hasOwnProperty(_formatAttribute(attr))
  ) {
    return true
  } else {
    return false
  }
}

/* Find the correct icon based on various Metacard attributes. */
function _deriveIconByMetacard(metacard) {
  let prop,
    dataTypes,
    metacardType,
    mimeType,
    contentType,
    contentTypeVersion,
    icon = _default

  prop = metacard.get('metacard').get('properties')
  dataTypes = prop.get('datatype')
  metacardType = _formatAttribute(prop.get('metacard-type'))
  mimeType = _formatAttribute(prop.get('media.type'))
  contentType = _formatAttribute(prop.get('metadata-content-type'))

  if (mimeType !== undefined) {
    const mime = mimeType.split('/')
    if (mime && mime.length === 2) {
      mimeType = mime[0]
    }
  }

  if (_iconExistsInMap(dataTypes, _map)) {
    icon = _get(_map, _formatAttribute(dataTypes[0]), _default)
  } else if (_iconExistsInMap(metacardType, _map)) {
    icon = _get(_map, metacardType, _default)
  } else if (_iconExistsInMap(contentType, _map)) {
    icon = _get(_map, contentType, _default)
  } else if (_iconExistsInMap(mimeType, _mimeMap)) {
    icon = _get(_mimeMap, mimeType, _default)
  }
  return icon
}

/* Find the correct icon by icon name. */
function _deriveIconByName(name) {
  return _get(_map, _formatAttribute(name), _default)
}

module.exports = {
  getClass(metacard) {
    const i = _deriveIconByMetacard(metacard)
    return _get(i, 'class', _default.class)
  },
  getUnicode(metacard) {
    const i = _deriveIconByMetacard(metacard)
    return _get(_map, 'style.code', _default.style.code)
  },
  getFont(metacard) {
    const i = _deriveIconByMetacard(metacard)
    return _get(_map, 'style.font', _default.style.font)
  },
  getSize(metacard) {
    const i = _deriveIconByMetacard(metacard)
    return _get(_map, 'style.size', _default.style.size)
  },
  getFull(metacard) {
    const i = _deriveIconByMetacard(metacard)
    return i !== undefined ? i : _default
  },
  getClassByName(name) {
    const i = _deriveIconByName(name)
    return _get(i, 'class', _default.class)
  },
}
