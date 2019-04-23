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
import * as React from 'react'
const sources = require('../../../component/singletons/sources-instance')
import { Model, Result, Props } from '.'
import { MetacardInteraction } from '../../presentation/metacard-interactions/metacard-interactions'
import { hot } from 'react-hot-loader'

const openValidUrl = (result: Result) => {
  const downloadUrl = result
    .get('metacard')
    .get('properties')
    .get('resource-download-url')
  downloadUrl && window.open(downloadUrl)
}

const isDownloadable = (model: Model): boolean =>
  model.some((result: Result) =>
    result
      .get('metacard')
      .get('properties')
      .get('resource-download-url')
  )

const handleDownload = (model: Model) => {
  model.forEach(openValidUrl)
}

const DownloadProduct = ({ model }: Props) => {
  if (!isDownloadable(model)) {
    return null
  }
  return (
    <MetacardInteraction
      text="Download"
      help="Downloads the result's associated product directly to your machine."
      icon="fa fa-download"
      onClick={() => handleDownload(model)}
    >
      {isRemoteResourceCached(model) && (
        <span
          data-help="Displayed if the remote resource has been cached locally."
          className="download-cached"
        >
          Local
        </span>
      )}
    </MetacardInteraction>
  )
}

const isRemoteResourceCached = (model: Model): boolean => {
  if (!model) return false

  const modelJson = model.toJSON()

  if (!modelJson || modelJson.length <= 0) return false

  return (
    modelJson[0].isResourceLocal &&
    modelJson[0].metacard.properties['source-id'] !== sources.localCatalog
  )
}

export default hot(module)(DownloadProduct)
