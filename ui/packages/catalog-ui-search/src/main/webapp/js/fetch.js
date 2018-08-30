module.exports = (url, { headers, ...opts } = {}) =>
  window.fetch(url, {
    credentials: 'same-origin',
    ...opts,
    headers: {
      'X-Requested-With': 'XMLHttpRequest',
      ...headers,
    },
  })
