import * as React from 'react'
import { useBackbone } from '../selection-checkbox/useBackbone.hook'
import { LazyQueryResult } from '../../js/model/LazyQueryResult/LazyQueryResult'
import { LazyQueryResults } from '../../js/model/LazyQueryResult/LazyQueryResults'
import {
  useStatusOfLazyResults,
  useSelectedResults,
} from '../../js/model/LazyQueryResult/hooks'

type useLazyResultsProps = {
  selectionInterface: any
}

export type LazyResultsType = {
  [key: string]: LazyQueryResult
}

const getLazyResultsFromSelectionInterface = ({
  selectionInterface,
}: useLazyResultsProps) => {
  const currentSearch = selectionInterface.get('currentQuery')
  if (!currentSearch) {
    return new LazyQueryResults()
  }
  const result = currentSearch.get('result')
  if (!result) {
    return new LazyQueryResults()
  }
  return result.get('lazyResults') as LazyQueryResults
}

export const useLazyResultsSelectedResultsFromSelectionInterface = ({
  selectionInterface,
}: useLazyResultsProps) => {
  const lazyResults = useLazyResultsFromSelectionInterface({
    selectionInterface,
  })
  const selectedResults = useSelectedResults({ lazyResults })

  return selectedResults
}

export const useLazyResultsStatusFromSelectionInterface = ({
  selectionInterface,
}: useLazyResultsProps) => {
  const lazyResults = useLazyResultsFromSelectionInterface({
    selectionInterface,
  })
  const status = useStatusOfLazyResults({ lazyResults })

  return status
}

export const useLazyResultsFromSelectionInterface = ({
  selectionInterface,
}: useLazyResultsProps) => {
  const { listenToOnce, stopListening } = useBackbone()
  //@ts-ignore
  const [forceRender, setForceRender] = React.useState(Math.random())
  const [lazyResults, setLazyResults] = React.useState(
    getLazyResultsFromSelectionInterface({
      selectionInterface,
    })
  )

  React.useEffect(
    () => {
      const unsubscribe = lazyResults.subscribeTo({
        subscribableThing: 'filteredResults',
        callback: () => {
          setForceRender(Math.random())
        },
      })
      return () => {
        unsubscribe()
      }
    },
    [lazyResults]
  )
  React.useEffect(
    () => {
      setLazyResults(
        getLazyResultsFromSelectionInterface({ selectionInterface })
      )
      listenToOnce(selectionInterface, 'change:currentQuery>result', () => {
        const currentQuery = selectionInterface.get('currentQuery')
        const result = currentQuery.get('result')
        if (result) {
          setLazyResults(
            getLazyResultsFromSelectionInterface({ selectionInterface })
          )
        }
      })
      return () => {
        stopListening(selectionInterface)
      }
    },
    [selectionInterface]
  )
  return lazyResults
}
