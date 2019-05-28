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

module.exports = DropdownView.extend({
  events: {}, // remove base events
  template() {
    const metadataThumbnail = this.modelForComponent
      .get('metacard')
      .get('properties')
      .get('thumbnail')
    const model = this.model

    const openThumbnailInNewWindow = () =>
      window.open(Common.getImageSrc(metadataThumbnail))
    const onMouseEnter = () => user.getHoverPreview() && model.open()
    const onMouseLeave = () => model.close()
    const onImageError = () => {
      this.imageLoadError = true
      this.render()
    }

    return (
      (metadataThumbnail && (
        <React.Fragment>
          {(this.imageLoadError && (
            <span>
              <i className="fa fa-picture-o" aria-hidden="true" />
              {` Unable to load image`}
            </span>
          )) || (
            <img
              src={Common.getImageSrc(metadataThumbnail)}
              onError={onImageError}
            />
          )}
          <button
            className="is-primary"
            onMouseEnter={onMouseEnter}
            onMouseLeave={onMouseLeave}
            onClick={openThumbnailInNewWindow}
            title="Click to open image in a new window."
          >
            <span className="fa fa-search-plus" />
          </button>
        </React.Fragment>
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
