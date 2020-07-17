var EventSource = require('eventsource')
const Common = require('./Common.js')

const REQUEST_HEADER = 'XMLHttpRequest'
const REQUEST_PATH = './internal/events/'
const HEADERS = {
  'X-Requested-With': REQUEST_HEADER,
}

import { EventType } from '../react-component/utils/event'

var source

var id = Common.generateUUID()

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
    if (source) {
      source.addEventListener(type, event => {
        onMessage(event)
      })
    } else {
      id = Common.generateUUID()
      source = new EventSource(REQUEST_PATH + id, {
        withCredentials: true,
        credentials: 'same-origin',
        headers: HEADERS,
      })
      source.addEventListener(type, event => {
        onMessage(event)
      })
      source.onerror = () => {
        source.close()
        source = null
      }
      source.addEventListener(EventType.Close, () => {
        source.close()
        source = null
      })
    }
  },
}
