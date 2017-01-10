import { expect } from 'chai'

import { start, end, fetch, setException } from './'

describe('fetch', () => {
  it('should fetch without any issues', async () => {
    const client = async (url, opts) => ({
      status: 200,
      json: () => true
    })

    let actions = []
    const dispatch = (action) => actions.push(action)

    const res = await fetch('/', { id: 0 }, client)(dispatch)
    const json = await res.json()
    expect(json).to.be.true
    expect(actions).to.deep.equal([start(0), end(0)])
  })

  it('should fetch and log exception', async () => {
    const client = async (url, opts) => ({
      status: 500,
      json: () => ({ key: 'value' })
    })

    let actions = []
    const dispatch = (action) => actions.push(action)

    try {
      await fetch('/', { id: 0 }, client)(dispatch)
    } catch (e) {
      expect(e.message).to.equal('Internal Server Error')
      expect(actions).to.deep.equal([
        start(0),
        end(0),
        setException({ url: '/', key: 'value' })
      ])
    }
  })
})
