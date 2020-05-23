import * as React from 'react'
import { useBackbone } from '../selection-checkbox/useBackbone.hook'
import { LazyQueryResult } from '../../js/model/LazyQueryResult'

type useLazyResultsProps = {
  selectionInterface: any
}

export type LazyResultsType = {
  [key: string]: LazyQueryResult
}

const getLazyResultsFromSelectionInterface = ({
  selectionInterface,
}: useLazyResultsProps): LazyResultsType => {
  const currentSearch = selectionInterface.get('currentQuery')
  if (!currentSearch) {
    return {}
  }
  const result = currentSearch.get('result')
  if (!result) {
    return {}
  }
  return result.lazyResults
}

const calcProgress = ({ selectionInterface }: useLazyResultsProps) => {
  const result = selectionInterface.get('currentQuery').get('result')
  return (
    (result.processedResults.length /
      (result.queuedResults.length + result.processedResults.length)) *
    100
  )
}

export const useLazyResultProcessingDetails = ({
  selectionInterface,
}: useLazyResultsProps) => {
  const [processingDetails, setProcessingDetails] = React.useState(
    'not processing' as
      | 'not processing'
      | {
          progress: number
          processed: number
          queued: number
          total: number
        }
  )
  const lazyResultStatus = useLazyResultStatus({ selectionInterface })
  React.useEffect(
    () => {
      if (lazyResultStatus === 'processing') {
        const result = selectionInterface.get('currentQuery').get('result')
        const timeoutId = setInterval(() => {
          setProcessingDetails({
            progress: calcProgress({ selectionInterface }),
            processed: result.processedResults.length,
            queued: result.queuedResults.length,
            total: result.queuedResults.length + result.processedResults.length,
          })
        }, 100)
        return () => {
          clearInterval(timeoutId)
          setProcessingDetails('not processing')
        }
      }
    },
    [lazyResultStatus]
  )
  return processingDetails
}

export const useLazyResultStatus = ({
  selectionInterface,
}: useLazyResultsProps) => {
  const [status, setStatus] = React.useState('normal' as
    | 'normal'
    | 'searching'
    | 'processing')
  const { listenTo, listenToOnce } = useBackbone()

  React.useEffect(() => {
    const listenToCurrentQuery = () => {
      if (selectionInterface.get('currentQuery')) {
        listenTo(
          selectionInterface.get('currentQuery'),
          'sync:result request:result error:result',
          () => {
            const currentQuery = selectionInterface.get('currentQuery')
            const result = currentQuery.get('result')
            if (result) {
              if (result.isSearching() && !result.isJustSearching()) {
                setStatus('processing')
              } else if (result.isSearching()) {
                setStatus('searching')
              } else {
                setStatus('normal')
              }
            } else {
              setStatus('normal')
            }
          }
        )
      }
    }
    if (selectionInterface.get('currentQuery')) {
      listenToCurrentQuery()
    } else {
      listenToOnce(selectionInterface, 'change:currentQuery', () => {
        listenToCurrentQuery()
      })
    }
  }, [])

  return status
}

export const useLazyResults = ({ selectionInterface }: useLazyResultsProps) => {
  const { listenTo, listenToOnce } = useBackbone()
  const [lazyResults, setLazyResults] = React.useState(
    getLazyResultsFromSelectionInterface({
      selectionInterface,
    })
  )
  const lazyResultsRef = React.useRef(lazyResults)
  const smartSet = React.useMemo(() => {
    return (update: LazyResultsType) => {
      if (
        Object.keys(lazyResultsRef.current).length === 0 &&
        Object.keys(update).length === 0
      ) {
        return
      }
      setLazyResults({
        ...update,
      })
    }
  }, [])
  React.useEffect(() => {
    const listenToCurrentQuery = () => {
      if (selectionInterface.get('currentQuery')) {
        listenTo(
          selectionInterface.get('currentQuery'),
          'sync:result request:result error:result',
          () => {
            const currentQuery = selectionInterface.get('currentQuery')
            const result = currentQuery.get('result')
            if (result) {
              smartSet(
                getLazyResultsFromSelectionInterface({ selectionInterface })
              )
              if (result.isSearching() && !result.isJustSearching()) {
                smartSet(
                  getLazyResultsFromSelectionInterface({ selectionInterface })
                )
              }
            }
          }
        )
      }
    }
    if (selectionInterface.get('currentQuery')) {
      listenToCurrentQuery()
    } else {
      listenToOnce(selectionInterface, 'change:currentQuery', () => {
        listenToCurrentQuery()
      })
    }
  }, [])
  return lazyResults
}

const resultsToMap = (results: LazyQueryResult[]) => {
  return results.reduce(
    (blob, result) => {
      blob[result['metacard.id']] = result
      return blob
    },
    {} as { [key: string]: LazyQueryResult }
  )
}

type useLazySelectionInterfaceReturnType = {
  collection: {
    [key: string]: LazyQueryResult
  }
  add: (items: LazyQueryResult[]) => void
  remove: (items: LazyQueryResult[]) => void
  toggle: (items: LazyQueryResult[]) => void
  set: (items: LazyQueryResult[]) => void
  clear: () => void
}

const useLazySelectionInterfaceStore = {} as {
  [key: string]: {
    [key: string]: useLazySelectionInterfaceReturnType & {
      setCollection: React.Dispatch<
        React.SetStateAction<{
          [key: string]: LazyQueryResult
        }>
      >
    }
  }
}

/**
 * Allow passing a key to sync two different hook calls
 *
 * Sure we could do this we context, prop drilling, redux, whatever, but those are
 * setup for an app that's entirely in react, and we aren't quite there yet. This will be robust and clean
 * enough for now.
 *
 * Key should be some sort of unique identifier (such as a query id!)
 */
export const useLazySelectionInterface = ({
  initialSelection = [],
  key,
}: {
  initialSelection?: LazyQueryResult[]
  key?: string
} = {}): useLazySelectionInterfaceReturnType => {
  const [instanceId] = React.useState(Math.random().toString())
  const [collection, setCollection] = React.useState(
    resultsToMap(initialSelection)
  )

  const updateOtherInstances = (update: { [key: string]: LazyQueryResult }) => {
    if (key !== undefined) {
      const otherInstancesMap = useLazySelectionInterfaceStore[key]
      Object.keys(otherInstancesMap)
        .filter(otherInstanceId => otherInstanceId !== instanceId)
        .forEach(otherInstanceId => {
          otherInstancesMap[otherInstanceId].setCollection(update)
        })
    }
  }

  const add = (results: LazyQueryResult[]) => {
    const resultsToAdd = resultsToMap(results)
    const update = {
      ...collection,
      ...resultsToAdd,
    }
    results
      .filter(result => {
        return collection[result['metacard.id']] === undefined
      })
      .forEach(result => {
        result.setSelected(true)
      })
    setCollection(update)
    updateOtherInstances(update)
  }

  const remove = (results: LazyQueryResult[]) => {
    results.forEach(result => {
      delete collection[result['metacard.id']]
      result.setSelected(false)
    })
    const update = {
      ...collection,
    }
    setCollection(update)
    updateOtherInstances(update)
  }

  const toggle = (results: LazyQueryResult[]) => {
    results.forEach(result => {
      if (collection[result['metacard.id']]) {
        delete collection[result['metacard.id']]
        result.setSelected(false)
      } else {
        collection[result['metacard.id']] = result
        result.setSelected(true)
      }
    })
    const update = { ...collection }
    setCollection(update)
    updateOtherInstances(update)
  }

  const clear = () => {
    const update = {}
    Object.values(collection).forEach(result => {
      result.setSelected(false)
    })
    setCollection(update)
    updateOtherInstances(update)
  }

  const set = (results: LazyQueryResult[]) => {
    const update = {
      ...resultsToMap(results),
    }
    Object.values(collection)
      .filter(result => {
        return update[result['metacard.id']] === undefined
      })
      .forEach(result => {
        result.setSelected(false)
      })
    results
      .filter(result => {
        return collection[result['metacard.id']] === undefined
      })
      .forEach(result => {
        result.setSelected(true)
      })
    setCollection(update)
    updateOtherInstances(update)
  }

  const instance = {
    collection,
    set,
    add,
    remove,
    toggle,
    clear,
  }

  React.useEffect(() => {
    if (key !== undefined) {
      if (useLazySelectionInterfaceStore[key] === undefined) {
        useLazySelectionInterfaceStore[key] = {}
      }
      useLazySelectionInterfaceStore[key][instanceId] = {
        ...instance,
        setCollection,
      }
    }
  })

  React.useEffect(() => {
    return () => {
      if (key !== undefined) {
        delete useLazySelectionInterfaceStore[key][instanceId]
      }
    }
  }, [])

  return instance
}

export const useLazyResultSelection = ({
  lazyResult,
  key,
}: {
  lazyResult: LazyQueryResult
  key: string
}) => {
  const lazySelectionInterface = useLazySelectionInterface({ key })

  return (
    lazySelectionInterface.collection[lazyResult['metacard.id']] !== undefined
  )
}
