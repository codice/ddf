var TextDecoderStream = global.TextDecoderStream
var TransformStream = global.TransformStream
const ORIGIN_HEADER = 'https://localhost:8993/'
const REQUEST_HEADER = 'XMLHttpRequest'
const HEADERS = {
  Origin: ORIGIN_HEADER,
  'X-Requested-With': REQUEST_HEADER,
}

function FetchEventTarget(input, init) {
  const eventTarget = new EventTarget()
  const jsonDecoder = makeJsonDecoder(input)
  const eventStream = makeWriteableEventStream(eventTarget)
  fetch(input, init)
    .then(response => {
      response.body
        .pipeThrough(new TextDecoderStream())
        .pipeThrough(jsonDecoder)
        .pipeTo(eventStream)
    })
    .catch(error => {
      eventTarget.dispatchEvent(new CustomEvent('error', { detail: error }))
    })
  return eventTarget
}

function makeJsonDecoder() {
  return new TransformStream({
    start(controller) {
      controller.buf = ''
      controller.pos = 0
    },
    transform(chunk, controller) {
      controller.buf += chunk
      while (controller.pos < controller.buf.length) {
        if (controller.buf[controller.pos] == '\n') {
          const line = controller.buf.substring(0, controller.pos)
          console.log(line)
          if (line !== ': ') {
            controller.enqueue(JSON.parse(line))
          }
          controller.buf = controller.buf.substring(controller.pos + 1)
          controller.pos = 0
        } else {
          ++controller.pos
        }
      }
    },
  })
}

function makeWriteableEventStream(eventTarget) {
  return new WritableStream({
    start() {
      eventTarget.dispatchEvent(new Event('start'))
    },
    write(message) {
      eventTarget.dispatchEvent(
        new MessageEvent(message.type, { data: message.data })
      )
    },
    close() {
      eventTarget.dispatchEvent(new CloseEvent('close'))
    },
    abort(reason) {
      eventTarget.dispatchEvent(new CloseEvent('abort', { reason }))
    },
  })
}

const eventTarget = () => {
  var abortController = new AbortController()
  const eventTarget = new FetchEventTarget('./internal/events', {
    method: 'POST',
    headers: new Headers(HEADERS),
    mode: 'same-origin',
    signal: abortController.signal,
  })
  return eventTarget
}

export default eventTarget()
