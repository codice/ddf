import fetch from '../fetch'

type Query = {
  srcs?: string[]
  count?: number
  cql: string
  facets?: string[]
}

const query = async (q: Query) => {
  const res = await fetch('./internal/cql', {
    method: 'POST',
    body: JSON.stringify(q),
  })

  return res.json()
}

export default query
