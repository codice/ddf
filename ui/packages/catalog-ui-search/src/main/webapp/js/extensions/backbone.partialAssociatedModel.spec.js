/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
import { expect } from 'chai'
import PartialAssociatedModel from './backbone.partialAssociatedModel'
import * as Backbone from 'backbone-associations'
import fetch from '../../react-component/utils/fetch'

const sleep = timeout => new Promise(resolve => setTimeout(resolve, timeout))
const generateMockFetch = (data, options = { delay: 0, multi: false }) => {
  return async url => {
    return {
      json: async () => {
        await sleep(options.delay)
        if (options.multi) {
          return data[url]
        } else {
          return data
        }
      },
    }
  }
}

describe('Backbone Partial Associated Model', () => {
  it('identifies partial models', () => {
    const ParentModel = PartialAssociatedModel.extend({})
    const instanceParent = new ParentModel({ id: 1 })
    expect(instanceParent.isPartial()).to.equal(true)
  })

  it('identifies non-partial models', () => {
    const ParentModel = PartialAssociatedModel.extend({})
    const instanceParent = new ParentModel({ id: 1, name: 'rick' })
    expect(instanceParent.isPartial()).to.equal(false)
  })

  it('identifies partial models (relations)', () => {
    const ParentModel = PartialAssociatedModel.extend({
      relations: [
        {
          type: Backbone.Many,
          key: 'subthing',
          relatedModel: PartialAssociatedModel,
        },
      ],
    })
    const instanceParent = new ParentModel({
      id: 1,
      name: 'rick',
      subthing: [{ id: 1 }],
    })
    expect(instanceParent.isPartial()).to.equal(true)
  })

  it('identifies non-partial models with partial relations', () => {
    const Model = PartialAssociatedModel.extend({
      relations: [
        {
          type: Backbone.Many,
          key: 'subthing',
          relatedModel: PartialAssociatedModel,
        },
      ],
    })
    const instanceParent = new Model({
      id: 1,
      name: 'rick',
      subthing: [{ id: 1, name: 'rick' }],
    })
    expect(instanceParent.isPartial()).to.equal(false)
  })

  it('fetches partial models', done => {
    const ParentModel = PartialAssociatedModel.extend({
      urlRoot: './parents',
    })
    const instanceParent = new ParentModel(
      { id: 1 },
      { fetch: generateMockFetch({ name: 'rick' }) }
    )
    instanceParent.once('partialSync', () => {
      expect(instanceParent.isPartial()).to.equal(false)
      done()
    })
    if (instanceParent.isPartial()) {
      instanceParent.fetchPartial()
    }
  })

  it('fetches partial models more than once', done => {
    const ParentModel = PartialAssociatedModel.extend({
      urlRoot: './parents',
    })
    const instanceParent = new ParentModel(
      { id: 1 },
      { fetch: generateMockFetch({ name: 'rick' }) }
    )
    instanceParent.once('partialSync', () => {
      expect(instanceParent.isPartial()).to.equal(false)
      instanceParent.once('partialSync', () => {
        expect(instanceParent.isPartial()).to.equal(false)
        done()
      })
      setTimeout(() => {
        instanceParent.fetchPartial()
      }, 100)
    })
    if (instanceParent.isPartial()) {
      instanceParent.fetchPartial()
    }
  })

  it('fetches partial relations with Many relationship', done => {
    const ParentModel = PartialAssociatedModel.extend({
      urlRoot: './parents',
      relations: [
        {
          type: Backbone.Many,
          key: 'subthing',
          relatedModel: PartialAssociatedModel,
        },
      ],
    })
    const instanceParent = new ParentModel(
      { id: 1, name: 'rick', subthing: [{ id: 2 }, { id: 3 }] },
      {
        fetch: generateMockFetch(
          {
            './parents/1': {
              name: 'rick',
            },
            './parents/1/subthing': [
              {
                id: 2,
                name: 'rick2',
              },
              {
                id: 3,
                name: 'rick3',
              },
            ],
          },
          { multi: true }
        ),
      }
    )
    instanceParent.once('partialSync', () => {
      expect(instanceParent.isPartial()).to.equal(false)
      done()
    })
    if (instanceParent.isPartial()) {
      instanceParent.fetchPartial()
    }
  })

  it('fetches partial relations with One relationship', done => {
    const ParentModel = PartialAssociatedModel.extend({
      urlRoot: './parents',
      relations: [
        {
          type: Backbone.One,
          key: 'subthing',
          relatedModel: PartialAssociatedModel,
        },
      ],
    })
    const instanceParent = new ParentModel(
      { id: 1, name: 'rick', subthing: { id: 2 } },
      {
        fetch: generateMockFetch(
          {
            './parents/1': {
              name: 'rick',
            },
            './parents/1/subthing': {
              id: 2,
              name: 'rick2',
            },
          },
          { multi: true }
        ),
      }
    )
    instanceParent.once('partialSync', () => {
      expect(instanceParent.isPartial()).to.equal(false)
      done()
    })
    if (instanceParent.isPartial()) {
      instanceParent.fetchPartial()
    }
  })
})
