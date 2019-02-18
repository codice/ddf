import * as React from 'react'
import * as sources from '../../../component/singletons/sources-instance'
import { Model, Result } from '.'
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

const DownloadProduct = (props: any) => {
  if (!isDownloadable(props.model)) {
    return null
  }
  return (
    <MetacardInteraction
      text="Download"
      help="Downloads the result's associated product directly to your machine."
      icon="fa fa-download"
      onClick={() => handleDownload(props.model)}
    >
      {isRemoteResourceCached(props.model) && (
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
