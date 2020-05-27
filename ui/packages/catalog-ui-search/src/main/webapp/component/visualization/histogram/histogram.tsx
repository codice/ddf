import * as React from 'react'
import { hot } from 'react-hot-loader'
import MRC from '../../../react-component/marionette-region-container'
import { useLazyResultsFromSelectionInterface } from '../../selection-interface/hooks'
import { LazyQueryResults } from '../../../js/model/LazyQueryResult/LazyQueryResults'
import { LazyQueryResult } from '../../../js/model/LazyQueryResult/LazyQueryResult'
import { useBackbone } from '../../selection-checkbox/useBackbone.hook'
import { useSelectedResults } from '../../../js/model/LazyQueryResult/hooks'

const wreqr = require('../../../js/wreqr.js')
const $ = require('jquery')
const _ = require('underscore')
const Plotly = require('plotly.js/dist/plotly.js')
const Property = require('../../property/property.js')
const PropertyView = require('../../property/property.view.js')
const metacardDefinitions = require('../../singletons/metacard-definitions.js')
const Common = require('../../../js/Common.js')
const properties = require('../../../js/properties.js')
const moment = require('moment')
const user = require('../../singletons/user-instance.js')

const zeroWidthSpace = '\u200B'
const plotlyDateFormat = 'YYYY-MM-DD HH:mm:ss.SS'

function getPlotlyDate(date: string) {
  return moment(date).format(plotlyDateFormat)
}

function calculateAvailableAttributes(results: LazyQueryResult[]) {
  let availableAttributes = [] as string[]
  results.forEach(result => {
    availableAttributes = _.union(
      availableAttributes,
      Object.keys(result.plain.metacard.properties)
    )
  })
  return availableAttributes
    .filter(
      attribute => metacardDefinitions.metacardTypes[attribute] !== undefined
    )
    .filter(attribute => !metacardDefinitions.isHiddenType(attribute))
    .filter(attribute => !properties.isHidden(attribute))
    .map(attribute => ({
      label: metacardDefinitions.metacardTypes[attribute].alias || attribute,
      value: attribute,
    }))
}

function calculateAttributeArray({
  results,
  attribute,
}: {
  results: LazyQueryResult[]
  attribute: string
}) {
  const values = [] as string[]
  results.forEach(result => {
    if (metacardDefinitions.metacardTypes[attribute].multivalued) {
      const resultValues = result.plain.metacard.properties[attribute]
      if (resultValues && resultValues.forEach) {
        resultValues.forEach((value: any) => {
          addValueForAttributeToArray({ valueArray: values, attribute, value })
        })
      } else {
        addValueForAttributeToArray({
          valueArray: values,
          attribute,
          value: resultValues,
        })
      }
    } else {
      addValueForAttributeToArray({
        valueArray: values,
        attribute,
        value: result.plain.metacard.properties[attribute],
      })
    }
  })
  return values
}

function findMatchesForAttributeValues(
  results: LazyQueryResult[],
  attribute: string,
  values: any[]
) {
  return results.filter(result => {
    if (metacardDefinitions.metacardTypes[attribute].multivalued) {
      const resultValues = result.plain.metacard.properties[attribute]
      if (resultValues && resultValues.forEach) {
        for (let i = 0; i < resultValues.length; i++) {
          if (checkIfValueIsValid(values, attribute, resultValues[i])) {
            return true
          }
        }
        return false
      } else {
        return checkIfValueIsValid(values, attribute, resultValues)
      }
    } else {
      return checkIfValueIsValid(
        values,
        attribute,
        result.plain.metacard.properties[attribute]
      )
    }
  })
}

function checkIfValueIsValid(values: any[], attribute: string, value: any) {
  if (value !== undefined) {
    switch (metacardDefinitions.metacardTypes[attribute].type) {
      case 'DATE':
        const plotlyDate = getPlotlyDate(value)
        return plotlyDate >= values[0] && plotlyDate <= values[1]
      case 'BOOLEAN':
      case 'STRING':
      case 'GEOMETRY':
        return values.indexOf(value.toString() + zeroWidthSpace) >= 0
      default:
        return value >= values[0] && value <= values[1]
    }
  }
}

function addValueForAttributeToArray({
  valueArray,
  attribute,
  value,
}: {
  valueArray: any[]
  attribute: string
  value: any
}) {
  if (value !== undefined) {
    switch (metacardDefinitions.metacardTypes[attribute].type) {
      case 'DATE':
        valueArray.push(getPlotlyDate(value))
        break
      case 'BOOLEAN':
      case 'STRING':
      case 'GEOMETRY':
        valueArray.push(value.toString() + zeroWidthSpace)
        break
      default:
        valueArray.push(parseFloat(value))
        break
    }
  }
}

function getIndexClicked(data: any) {
  return Math.max.apply(
    undefined,
    data.points.map((point: any) => point.pointNumber)
  ) as number
}

function getValueFromClick(data: any, categories: any) {
  switch (data.points[0].xaxis.type) {
    case 'category':
      return [data.points[0].x]
    case 'date':
      const currentDate = moment(data.points[0].x).format(plotlyDateFormat)
      return _.find(categories, (category: any) => {
        return currentDate >= category[0] && currentDate <= category[1]
      })
    default:
      return _.find(categories, (category: any) => {
        return (
          data.points[0].x >= category[0] && data.points[0].x <= category[1]
        )
      })
  }
}

function getTheme(theme: any) {
  const config = {
    margin: {
      t: 10,
      l: 50,
      r: 115,
      b: 90,
      pad: 0,
      autoexpand: true,
    },
  }
  switch (theme) {
    case 'comfortable':
      config.margin.b = 140
      return config
    case 'cozy':
      config.margin.b = 115
      return config
    case 'compact':
      config.margin.b = 90
      return config
    default:
      return config
  }
}

function getLayout(plot?: any) {
  const prefs = user.get('user').get('preferences')
  const theme = getTheme(prefs.get('theme').get('spacingMode'))

  const baseLayout = {
    autosize: true,
    paper_bgcolor: 'rgba(0,0,0,0)',
    plot_bgcolor: 'rgba(0,0,0,0)',
    font: {
      family: '"Open Sans Light","Helvetica Neue",Helvetica,Arial,sans-serif',
      size: prefs.get('fontSize'),
      color: 'white',
    },
    margin: theme.margin,
    barmode: 'overlay',
    xaxis: {
      fixedrange: true,
    },
    yaxis: {
      fixedrange: true,
    },
    showlegend: true,
  } as any
  if (plot) {
    baseLayout.xaxis.autorange = false
    baseLayout.xaxis.range = plot._fullLayout.xaxis.range
    baseLayout.yaxis.range = plot._fullLayout.yaxis.range
    baseLayout.yaxis.autorange = false
  }
  return baseLayout
}

type Props = {
  selectionInterface: any
}

const getPropertyView = ({
  lazyResults,
  attributeToBin,
}: {
  lazyResults: LazyQueryResults
  attributeToBin: any
}) => {
  const propertyView = new PropertyView({
    model: new Property({
      showValidationIssues: false,
      enumFiltering: true,
      enum: calculateAvailableAttributes(
        Object.values(lazyResults.filteredResults)
      ),
      value: [attributeToBin],
      id: 'Group by',
    }),
  })
  propertyView.turnOnEditing()

  return propertyView
}

export const Histogram = ({ selectionInterface }: Props) => {
  const { listenTo, stopListening } = useBackbone()
  const [noMatchingData, setNoMatchingData] = React.useState(false)
  const plotlyRef = React.useRef<HTMLDivElement>()
  const lazyResults = useLazyResultsFromSelectionInterface({
    selectionInterface,
  })
  const selectedResults = useSelectedResults({ lazyResults })
  const [attributeToBin, setAttributeToBin] = React.useState('' as string)
  const [propertyView, setPropertyView] = React.useState(
    getPropertyView({ lazyResults, attributeToBin })
  )
  const filteredResults = Object.values(lazyResults.filteredResults)
  React.useEffect(
    () => {
      listenTo(propertyView.model, 'change:value', () => {
        const newValue = propertyView.model.getValue()[0]
        if (newValue) {
          setAttributeToBin(newValue)
        }
      })
      return () => {
        stopListening(propertyView.model)
      }
    },
    [propertyView]
  )

  React.useEffect(
    () => {
      setNoMatchingData(false)
    },
    [lazyResults.filteredResults, attributeToBin]
  )

  React.useEffect(
    () => {
      setPropertyView(getPropertyView({ lazyResults, attributeToBin }))
    },
    [lazyResults.filteredResults]
  )

  React.useEffect(
    () => {
      showHistogram()
    },
    [lazyResults.filteredResults, attributeToBin]
  )

  React.useEffect(
    () => {
      updateHistogram()
    },
    [selectedResults]
  )

  const determineInitialData = () => {
    return [
      {
        x: calculateAttributeArray({
          results: filteredResults,
          attribute: attributeToBin,
        }),
        opacity: 1,
        type: 'histogram',
        name: 'Hits        ',
        marker: {
          color: 'rgba(255, 255, 255, .05)',
          line: {
            color: 'rgba(255,255,255,.2)',
            width: '2',
          },
        },
      },
    ]
  }

  const determineData = (plot: any) => {
    const activeResults = filteredResults
    const xbins = Common.duplicate(plot._fullData[0].xbins)
    if (xbins.size.constructor !== String) {
      xbins.end = xbins.end + xbins.size //https://github.com/plotly/plotly.js/issues/1229
    } else {
      // soooo plotly introduced this cool bin size shorthand where M3 means 3 months, M6 6 months etc.
      xbins.end =
        xbins.end + parseInt(xbins.size.substring(1)) * 31 * 24 * 3600000 //https://github.com/plotly/plotly.js/issues/1229
    }
    return [
      {
        x: calculateAttributeArray({
          results: activeResults,
          attribute: attributeToBin,
        }),
        opacity: 1,
        type: 'histogram',
        hoverinfo: 'y+x+name',
        name: 'Hits        ',
        marker: {
          color: 'rgba(255, 255, 255, .05)',
          line: {
            color: 'rgba(255,255,255,.2)',
            width: '2',
          },
        },
        autobinx: false,
        xbins,
      },
      {
        x: calculateAttributeArray({
          results: Object.values(selectedResults),
          attribute: attributeToBin,
        }),
        opacity: 1,
        type: 'histogram',
        hoverinfo: 'y+x+name',
        name: 'Selected',
        marker: {
          color: 'rgba(255, 255, 255, .2)',
        },
        autobinx: false,
        xbins,
      },
    ]
  }

  const handleResize = () => {
    if (plotlyRef.current) {
      const histogramElement = plotlyRef.current
      $(histogramElement)
        .find('rect.drag')
        .off('mousedown')
      //@ts-ignore
      if (histogramElement._context) {
        Plotly.Plots.resize(histogramElement)
      }
      $(histogramElement)
        .find('rect.drag')
        .on('mousedown', (event: any) => {
          shiftKey.current = event.shiftKey
          metaKey.current = event.metaKey
          ctrlKey.current = event.ctrlKey
        })
    }
  }

  const updateTheme = (e: any) => {
    if (plotlyRef.current) {
      const histogramElement = plotlyRef.current
      if (
        histogramElement.children.length !== 0 &&
        attributeToBin &&
        filteredResults.length !== 0
      ) {
        const theme = getTheme(e.get('spacingMode'))
        //@ts-ignore
        histogramElement.layout.margin = theme.margin
      }
    }
  }

  const updateFontSize = (e: any) => {
    if (plotlyRef.current) {
      const histogramElement = plotlyRef.current

      if (
        histogramElement.children.length !== 0 &&
        attributeToBin &&
        filteredResults.length !== 0
      ) {
        //@ts-ignore
        histogramElement.layout.font.size = e.get('fontSize')
      }
    }
  }

  React.useEffect(() => {
    listenTo(
      user.get('user').get('preferences'),
      'change:fontSize',
      updateFontSize
    )
    listenTo(user.get('user').get('preferences'), 'change:theme', updateTheme)
  }, [])

  React.useEffect(() => {
    const id = (Math.random() * 100).toFixed(0).toString()
    listenTo(wreqr.vent, 'resize', handleResize)
    $(window).on(`resize.${id}`, handleResize)
    return () => {
      $(window).off(`resize.${id}`)
    }
  }, [])

  const showHistogram = () => {
    if (plotlyRef.current) {
      if (filteredResults.length > 0 && attributeToBin.length > 0) {
        const histogramElement = plotlyRef.current
        const initialData = determineInitialData()
        if (initialData[0].x.length === 0) {
          setNoMatchingData(true)
        } else {
          Plotly.newPlot(histogramElement, initialData, getLayout(), {
            displayModeBar: false,
          }).then((plot: any) => {
            Plotly.newPlot(
              histogramElement,
              determineData(plot),
              getLayout(plot),
              {
                displayModeBar: false,
              }
            )
            handleResize()
            listenToHistogram()
          })
        }
      } else {
        plotlyRef.current.innerHTML = ''
      }
    }
  }

  const updateHistogram = () => {
    if (plotlyRef.current) {
      const histogramElement = plotlyRef.current
      if (
        histogramElement !== null &&
        histogramElement.children.length !== 0 &&
        attributeToBin &&
        filteredResults.length > 0
      ) {
        Plotly.deleteTraces(histogramElement, 1)
        Plotly.addTraces(histogramElement, determineData(histogramElement)[1])
        handleResize()
      } else {
        histogramElement.innerHTML = ''
      }
    }
  }

  const selectBetween = (firstIndex: number, lastIndex: number) => {
    for (let i = firstIndex; i <= lastIndex; i++) {
      if (pointsSelected.current.indexOf(i) === -1) {
        pointsSelected.current.push(i)
      }
    }
    const attributeToCheck = attributeToBin
    const categories = retrieveCategoriesFromPlotly()
    const validCategories = categories.slice(firstIndex, lastIndex)
    const activeSearchResults = filteredResults
    const results = validCategories.reduce(
      (results: any, category: any) => {
        results = results.concat(
          findMatchesForAttributeValues(
            activeSearchResults,
            attributeToCheck,
            category.constructor === Array ? category : [category]
          )
        )
        return results
      },
      [] as LazyQueryResult[]
    ) as LazyQueryResult[]
    results.forEach(result => {
      result.setSelected(true)
    })
  }

  const retrieveCategoriesFromPlotlyForDates = () => {
    if (plotlyRef.current) {
      const histogramElement = plotlyRef.current
      const categories = []
      //@ts-ignore
      const xbins = histogramElement._fullData[0].xbins
      const min = xbins.start
      const max = xbins.end
      let start = min
      const inMonths = xbins.size.constructor === String
      const binSize = inMonths ? parseInt(xbins.size.substring(1)) : xbins.size
      while (start < max) {
        const startDate = moment(start).format(plotlyDateFormat)
        const endDate = inMonths
          ? moment(start)
              .add(binSize, 'months')
              .format(plotlyDateFormat)
          : moment(start)
              .add(binSize, 'ms')
              .format(plotlyDateFormat)
        categories.push([startDate, endDate])
        start = parseInt(
          inMonths
            ? moment(start)
                .add(binSize, 'months')
                .format('x')
            : moment(start)
                .add(binSize, 'ms')
                .format('x')
        )
      }
      return categories
    }
  }

  // This is an internal variable for Plotly, so it might break if we update Plotly in the future.
  // Regardless, there was no other way to reliably get the categories.
  const retrieveCategoriesFromPlotly = () => {
    if (plotlyRef.current) {
      const histogramElement = plotlyRef.current
      //@ts-ignore
      const xaxis = histogramElement._fullLayout.xaxis
      switch (xaxis.type) {
        case 'category':
          return xaxis._categories
        case 'date':
          return retrieveCategoriesFromPlotlyForDates()
        default:
          //@ts-ignore
          const xbins = histogramElement._fullData[0].xbins
          const min = xbins.start
          const max = xbins.end
          const binSize = xbins.size
          const categories = []
          var start = min
          while (start < max) {
            categories.push([start, start + binSize])
            start += binSize
          }
          return categories
      }
    }
  }

  const handleControlClick = (data: any, alreadySelected: boolean) => {
    const attributeToCheck = attributeToBin
    const categories = retrieveCategoriesFromPlotly()
    const results = findMatchesForAttributeValues(
      filteredResults,
      attributeToCheck,
      getValueFromClick(data, categories)
    )
    if (alreadySelected) {
      results.forEach(result => {
        result.setSelected(false)
      })
      pointsSelected.current.splice(
        pointsSelected.current.indexOf(getIndexClicked(data)),
        1
      )
    } else {
      results.forEach(result => {
        result.setSelected(true)
      })
      pointsSelected.current.push(getIndexClicked(data))
    }
  }

  const handleShiftClick = (data: any) => {
    const indexClicked = getIndexClicked(data)
    const firstIndex =
      pointsSelected.current.length === 0
        ? -1
        : pointsSelected.current.reduce(
            (currentMin, point) => Math.min(currentMin, point),
            pointsSelected.current[0]
          )
    const lastIndex =
      pointsSelected.current.length === 0
        ? -1
        : pointsSelected.current.reduce(
            (currentMin, point) => Math.max(currentMin, point),
            pointsSelected.current[0]
          )
    if (firstIndex === -1 && lastIndex === -1) {
      lazyResults.deselect()
      handleControlClick(data, false)
    } else if (indexClicked <= firstIndex) {
      selectBetween(indexClicked, firstIndex)
    } else if (indexClicked >= lastIndex) {
      selectBetween(lastIndex, indexClicked + 1)
    } else {
      selectBetween(firstIndex, indexClicked + 1)
    }
  }

  const plotlyClickHandler = (data: any) => {
    console.log('heeee')
    const indexClicked = getIndexClicked(data)
    const alreadySelected = pointsSelected.current.indexOf(indexClicked) >= 0
    if (shiftKey.current) {
      handleShiftClick(data)
    } else if (ctrlKey.current || metaKey.current) {
      handleControlClick(data, alreadySelected)
    } else {
      lazyResults.deselect()
      resetPointSelection()
      handleControlClick(data, alreadySelected)
    }
    resetKeyTracking()
  }

  const listenToHistogram = () => {
    if (plotlyRef.current) {
      const histogramElement = plotlyRef.current
      //@ts-ignore
      histogramElement._ev.addListener('plotly_click', plotlyClickHandler)
    }
  }

  const shiftKey = React.useRef(false)
  const metaKey = React.useRef(false)
  const ctrlKey = React.useRef(false)
  const pointsSelected = React.useRef([] as number[])

  const resetKeyTracking = () => {
    shiftKey.current = false
    metaKey.current = false
    ctrlKey.current = false
  }

  const resetPointSelection = () => {
    pointsSelected.current = []
  }

  React.useEffect(() => {}, [])

  if (Object.keys(lazyResults.filteredResults).length === 0) {
    return <div style={{ padding: '20px' }}>No results found</div>
  }
  return (
    <>
      <div>
        {' '}
        <MRC view={propertyView} />
      </div>
      <div
        className="plotly-histogram"
        ref={plotlyRef as any}
        style={{
          height: 'calc(100% - 135px)',
          width: '100%',
          display: noMatchingData ? 'none' : 'block',
        }}
      />
      {noMatchingData ? (
        <div style={{ padding: '20px' }}>
          No data in this result set has that attribute
        </div>
      ) : null}
    </>
  )
}

export default hot(module)(Histogram)
