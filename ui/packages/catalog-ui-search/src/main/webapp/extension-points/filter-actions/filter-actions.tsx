import * as React from 'react'

type Props = {
  model: Backbone.Model
}

export default ({ model }: Props) => {
  return (
    <input
      onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
        model.set('extensionData', e.currentTarget.value)
      }}
    />
  )
}
