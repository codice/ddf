import { LazyQueryResult } from './LazyQueryResult'
import * as React from 'react'
import { LazyQueryResults } from './LazyQueryResults'
const _ = require('underscore')

/**
 * If a view cares about whether or not a lazy result is selected,
 * this will let them know.
 */
export const useSelectionOfLazyResult = ({
  lazyResult,
}: {
  lazyResult: LazyQueryResult
}) => {
  const [isSelected, setIsSelected] = React.useState(lazyResult.isSelected)
  React.useEffect(
    () => {
      const unsubscribe = lazyResult.subscribeTo({
        subscribableThing: 'selected',
        callback: () => {
          setIsSelected(lazyResult.isSelected)
        },
      })
      return () => {
        unsubscribe()
      }
    },
    [lazyResult]
  )
  return isSelected
}

/**
 * If a view cares about whether or not a lazy result is filtered,
 * this will let them know.
 */
export const useFilteredOfLazyResult = ({
  lazyResult,
}: {
  lazyResult: LazyQueryResult
}) => {
  const [isFiltered, setIsFiltered] = React.useState(lazyResult.isFiltered)
  React.useEffect(
    () => {
      const unsubscribe = lazyResult.subscribeTo({
        subscribableThing: 'filtered',
        callback: () => {
          setIsFiltered(lazyResult.isFiltered)
        },
      })
      return () => {
        unsubscribe()
      }
    },
    [lazyResult]
  )
  return isFiltered
}

type useSelectionOfLazyResultsReturn = 'unselected' | 'partially' | 'selected'

/**
 * Used by clusters to respond quickly to changes they care about
 * (in other words the results in their cluster)
 */
export const useSelectionOfLazyResults = ({
  lazyResults,
}: {
  lazyResults: LazyQueryResult[]
}) => {
  const cache = React.useRef({} as { [key: string]: boolean })
  const calculateIfSelected = React.useMemo(() => {
    return () => {
      const currentValues = Object.values(cache.current)
      let baseline = currentValues[0]
      let updateToIsSelected = baseline
        ? 'selected'
        : ('unselected' as useSelectionOfLazyResultsReturn)
      for (let i = 1; i <= currentValues.length - 1; i++) {
        if (baseline !== currentValues[i]) {
          updateToIsSelected = 'partially'
          break
        }
      }
      return updateToIsSelected
    }
  }, [])
  const debouncedUpdatedIsSelected = React.useMemo(() => {
    return _.debounce(() => {
      setIsSelected(calculateIfSelected())
    }, 100)
  }, [])

  const [isSelected, setIsSelected] = React.useState(
    calculateIfSelected() as useSelectionOfLazyResultsReturn
  )

  React.useEffect(
    () => {
      cache.current = lazyResults.reduce(
        (blob, lazyResult) => {
          blob[lazyResult['metacard.id']] = lazyResult.isSelected
          return blob
        },
        {} as { [key: string]: boolean }
      )
      setIsSelected(calculateIfSelected())
      const unsubscribeCalls = lazyResults.map(lazyResult => {
        return lazyResult.subscribeTo({
          subscribableThing: 'selected',
          callback: () => {
            cache.current[lazyResult['metacard.id']] = lazyResult.isSelected
            debouncedUpdatedIsSelected()
          },
        })
      })
      return () => {
        unsubscribeCalls.forEach(unsubscribeCall => {
          unsubscribeCall()
        })
      }
    },
    [lazyResults]
  )
  return isSelected
}

const getSelectedResultsOfLazyResults = ({
  lazyResults,
}: {
  lazyResults?: LazyQueryResults
}) => {
  if (lazyResults) {
    return {
      ...lazyResults.selectedResults,
    }
  }
  return {}
}

/**
 * If a view cares about the entirety of what results are selected out
 * of a LazyQueryResults object, this will keep them up to date.
 *
 * This is overkill for most components, but needed for things like
 * the inspector.  Most other components will instead respond to changes
 * in a single result.
 */
export const useSelectedResults = ({
  lazyResults,
}: {
  lazyResults?: LazyQueryResults
}) => {
  const [selectedResults, setSelectedResults] = React.useState(
    getSelectedResultsOfLazyResults({ lazyResults })
  )
  React.useEffect(
    () => {
      if (lazyResults) {
        const unsubscribeCall = lazyResults.subscribeTo({
          subscribableThing: 'selectedResults',
          callback: () => {
            setSelectedResults(getSelectedResultsOfLazyResults({ lazyResults }))
          },
        })
        return () => {
          unsubscribeCall()
        }
      }
      return () => {}
    },
    [lazyResults]
  )

  return selectedResults
}

const getStatusFromLazyResults = ({
  lazyResults,
}: {
  lazyResults: LazyQueryResults
}) => {
  return {
    status: lazyResults.status,
    isSearching: lazyResults.isSearching,
    currentAsOf: lazyResults.currentAsOf,
  }
}

/**
 * If a view cares about the status of a LazyQueryResults object
 */
export const useStatusOfLazyResults = ({
  lazyResults,
}: {
  lazyResults: LazyQueryResults
}) => {
  const [status, setStatus] = React.useState(
    getStatusFromLazyResults({ lazyResults })
  )
  React.useEffect(
    () => {
      setStatus(getStatusFromLazyResults({ lazyResults }))
      const unsubscribeCall = lazyResults.subscribeTo({
        subscribableThing: 'status',
        callback: () => {
          setStatus(getStatusFromLazyResults({ lazyResults }))
        },
      })
      return () => {
        unsubscribeCall()
      }
    },
    [lazyResults]
  )

  return status
}
