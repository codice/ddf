const Backbone = require('backbone')
import * as React from 'react'

export type WithBackboneProps = {
  listenTo: (object: any, events: string, callback: Function) => any
  stopListening: (
    object?: any,
    events?: string | undefined,
    callback?: Function | undefined
  ) => any
  listenToOnce: (object: any, events: string, callback: Function) => any
}

export function useBackbone(): WithBackboneProps {
  const backboneModel = new Backbone.Model({})
  React.useEffect(() => {
    return () => {
      backboneModel.stopListening()
      backboneModel.destroy()
    }
  }, [])
  return {
    listenTo: backboneModel.listenTo.bind(backboneModel),
    stopListening: backboneModel.stopListening.bind(backboneModel),
    listenToOnce: backboneModel.listenToOnce.bind(backboneModel),
  }
}
