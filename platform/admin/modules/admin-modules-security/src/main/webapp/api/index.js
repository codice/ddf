import 'whatwg-fetch'

export const list = () =>
  window.fetch('/admin/wizard', {credentials: 'same-origin'})
    .then((res) => res.json())

export const fetchStage = (id) =>
  window.fetch('/admin/wizard/' + id, {credentials: 'same-origin'})
    .then((res) => res.json())

export const submit = (stage, { method, url }) => {
  let opts = { method, credentials: 'same-origin' }

  if (method === 'POST') {
    opts.body = JSON.stringify(stage)
  }

  return window.fetch(url, opts)
    .then((res) => Promise.all([ res.status, res.json() ]))
}

