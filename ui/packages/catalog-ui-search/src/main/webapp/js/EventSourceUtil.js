var EventSource = require('eventsource')

const ORIGIN_HEADER = 'https://localhost:8993/'
const REQUEST_HEADER = 'XMLHttpRequest'
const HEADERS = {
  Origin: ORIGIN_HEADER,
  'X-Requested-With': REQUEST_HEADER,
}

var sources = []

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
    if (sources.length != 0) {
      sources[0].addEventListener(type, event => {
        onMessage(event)
      })
    } else {
      var source = new EventSource('./internal/events', {
        withCredentials: true,
        headers: HEADERS,
      })
      source.addEventListener(type, event => {
        onMessage(event)
      })
      sources.push(source)
    }
  },
}
