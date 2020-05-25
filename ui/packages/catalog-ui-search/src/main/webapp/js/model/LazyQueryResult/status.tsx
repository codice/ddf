import number from '../../../dev/component/number'

export type StatusBySourceType = {
  [key: string]: {
    count: number
    elapsed: number
    hits: number
    id: string
    successful: boolean
  }
}

export class Status {
  /**
   * Amount of results returned
   */
  count: number
  /**
   * Time the search took in milliseconds
   */
  elapsed: number
  /**
   * Total amount of results that match the search criteria
   * Not all are sent back, just what is in count, so this should always be
   * >= count
   */
  hits: number
  /**
   * Source name
   */
  id: string
  successful: boolean
  fromcache: number
  cacheHasReturned: boolean
  cacheSuccessful: boolean
  cacheMessages: []
  hasReturned: boolean
  messages: []
  constructor({ id }: { id: string }) {
    this.id = id
  }
  updateStatus(update: StatusBySourceType[0]) {
    Object.keys(update).forEach(key => {
      // @ts-ignore
      this[key] = update[key]
    })
  }
}
