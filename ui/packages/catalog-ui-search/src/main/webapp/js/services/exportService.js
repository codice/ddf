const exportDataAs = async (url, data, contentType) => {
  return await doFetch(url, {
    method: 'POST',
    body: JSON.stringify(data),
    headers: {
      'Content-Type': contentType,
    },
  })
}

const retrieveExportOptions = async () => {
  return await doFetch('./internal/cql/transforms')
}

const doFetch = async (url, config = {}) => {
  const headers = (config.headers && {
    ...config.headers,
    'X-Requested-With': 'XMLHttpRequest',
  }) || { 'X-Requested-With': 'XMLHttpRequest' }
  return fetch(url, { ...config, headers })
}

module.exports = { exportDataAs, retrieveExportOptions }
