import * as React from 'react'

type Props = {
  dataStore: any
}

export default ({ dataStore }: Props) => {
  return (
    <input
      onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
        dataStore.text = e.currentTarget.value
      }}
    />
  )
}
