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

import React from 'react'

const DropdownView = require('../dropdown.view')
const ComponentView = require('../../hover-preview/hover-preview.view.js')
const Common = require('../../../js/Common.js')
const user = require('../../singletons/user-instance.js')
const plugin = require('plugins/dropdown.hover-preview.view.js')

module.exports = DropdownView.extend({
  events: {}, // remove base events
  template() {
    const metacardProperties = this.modelForComponent
      .get('metacard')
      .get('properties')
    const model = this.model

    const openThumbnailInNewWindow = plugin(metacardProperties =>
      window.open(Common.getImageSrc(metacardProperties.get('thumbnail')))
    )

    const onMouseEnter = () => user.getHoverPreview() && model.open()
    const onMouseLeave = () => model.close()
    const onImageError = () => {
      this.imageLoadError = true
      this.render()
    }

    return (
      (metacardProperties.get('thumbnail') && (
        <div
          onClick={() => openThumbnailInNewWindow(metacardProperties)}
          title="Click to open image in a new window."
        >
          {(this.imageLoadError && (
            <span>
              <i className="fa fa-picture-o" aria-hidden="true" />
              {` Unable to load image`}
            </span>
          )) || (
            <img
              src={Common.getImageSrc(metacardProperties.get('thumbnail'))}
              onError={onImageError}
            />
          )}
          <button
            className="is-primary"
            onMouseEnter={onMouseEnter}
            onMouseLeave={onMouseLeave}
          >
            <span className="fa fa-search-plus" />
          </button>
        </div>
      )) || <React.Fragment />
    )
  },
  imageLoadError: false,
  className: 'is-hover-preview',
  componentToShow: ComponentView,
  initialize() {
    DropdownView.prototype.initialize.call(this)
  },
})
