import { EventSourcePolyfill } from 'event-source-polyfill'

const ORIGIN_HEADER = 'https://localhost:8993/'
const REQUEST_HEADER = 'XMLHttpRequest'
const HEADERS = {
  Origin: ORIGIN_HEADER,
  'X-Requested-With': REQUEST_HEADER,
}

const defaultHandlers = source => {
  return {
    onOpen: () => console.log('DEFAULT ON OPEN'),
    onMessage: () => console.log('DEFAULT ON MESSAGE'),
    onError: event => {
      console.log('DEFAULT ON ERROR')
      console.log(event)
      console.log(event.description)
      if (event.eventPhase == EventSource.CLOSED) {
        source.close()
        console.log('Event Source Closed')
      }
    },
  }
}

var sources = []

module.exports = {
  addListener(type, handler) {
    if (sources.length != 0) {
      sources[0].addEventListener(type, handler)
    } else {
      var EventSource = EventSourcePolyfill
      var source = new EventSource('./internal/events', {
        withCredentials: true,
        headers: HEADERS,
      })
      source.addEventListener(type, handler)
    }
  },
  createEventSource(handlers) {
    //Assign an ID to each source and return it (look into security)
    const ID =
      Math.random()
        .toString(36)
        .substring(2, 15) +
      Math.random()
        .toString(36)
        .substring(2, 15)

    var EventSource = EventSourcePolyfill
    var source = new EventSource('./internal/events', {
      withCredentials: true,
      headers: HEADERS,
    })

    const def = defaultHandlers(source)
    const {
      onOpen = def.onOpen,
      onMessage = def.onMessage,
      onError = def.onError,
    } = handlers

    // handle messages
    source.onmessage = onMessage
    source.onerror = onError
    source.onopen = onOpen

    sources[ID] = source
    console.log('IN CREATE, ID: ', ID)
    return ID
  },
}
