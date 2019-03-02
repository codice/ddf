import * as React from 'react'
import { MetacardInteraction } from '../../presentation/metacard-interactions/metacard-interactions'
import * as router from '../../../component/router/router'
import { Props } from '.'
import { hot } from 'react-hot-loader'
const wreqr = require('wreqr')

const ExpandMetacard = (props: Props) => {
  const isRouted = router && router.toJSON().name === 'openMetacard'

  if (isRouted || props.model.length > 1) {
    return null
  }

  return (
    <MetacardInteraction
      text="Expand Metacard View"
      help={`Takes you to a
              view that only focuses on this particular result. Bookmarking it will allow
              you to come back to this result directly.`}
      icon="fa fa-expand"
      onClick={() => handleExpand(props)}
    />
  )
}

const handleExpand = (props: Props) => {
  props.onClose()
  let id = props.model
    .first()
    .get('metacard')
    .get('properties')
    .get('id')

  id = encodeURIComponent(id)

  wreqr.vent.trigger('router:navigate', {
    fragment: 'metacards/' + id,
    options: {
      trigger: true,
    },
  })
}

export default hot(module)(ExpandMetacard)
