export type ResultType = {
  actions: ActionType[]
  distance: null
  hasThumbnail: boolean
  isResourceLocal: boolean
  matches: {}
  metacard: MetacardType
  relevance: number
  metacardType: string
  id: string
}

type MetacardType = {
  cached: string
  properties: MetacardPropertiesType
  id: string
}

export type MetacardPropertiesType = {
  id: string
  title: string
  'metacard.owner': string
  description: string
  created: string
  modified: string
  'security.access-individuals'?: string[]
  'security.access-individuals-read'?: string[]
  'security.access-groups'?: string[]
  'security.access-groups-read'?: string[]
  sorts?: string[] | { attribute: string; direction: string }[]
  [key: string]: any
}

type ActionType = {
  description: string
  displayName: string
  id: string
  title: string
  url: string
}
