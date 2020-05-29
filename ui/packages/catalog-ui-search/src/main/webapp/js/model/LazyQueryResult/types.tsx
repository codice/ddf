export type QuerySortType = {
  attribute: string
  direction: string
}

export type TruncatingFilterType = {
  type: 'ILIKE' | string
  property: string
  value: string
  filters: undefined
}

export type FilterType = {
  type: 'AND' | 'OR' | 'NOT AND' | 'NOT OR'
  filters: (TruncatingFilterType | FilterType)[]
}
