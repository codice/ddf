import eventTarget from './EventTarget'

// const defaultHandlers = source => {
//   return {
//     onOpen: () => console.log('DEFAULT ON OPEN'),
//     onMessage: () => console.log('DEFAULT ON MESSAGE'),
//     onError: event => {
//       console.log('DEFAULT ON ERROR')
//       console.log(event)
//       console.log(event.description)
//       if (event.eventPhase == EventSource.CLOSED) {
//         source.close()
//         console.log('Event Source Closed')
//       }
//     },
//   }
// }

// var sources = []

module.exports = {
  createEventListener(type, handlers) {
    //Assign an ID to each source and return it (look into security)
    // const ID =
    //   Math.random()
    //     .toString(36)
    //     .substring(2, 15) +
    //   Math.random()
    //     .toString(36)
    //     .substring(2, 15)

    const { onMessage } = handlers

    eventTarget.addEventListener(type, event => {
      onMessage(event)
    })

    // sources[ID] = eventTarget
    // console.log('IN CREATE, ID: ', ID)
    // return ID
  },
}
