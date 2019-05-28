const React = require('react')
const Announcement = require('../../component/announcement/index.jsx')

const AutoComplete = require('../auto-complete')
const Polygon = require('./polygon')
const MultiPolygon = require('./multipoly')

import fetch from '../../react-component/utils/fetch'

class Keyword extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      value: typeof props.value === 'string' ? props.value : '',
      loading: false,
      error: null,
      polyType: null,
    }
    this.fetch = this.props.fetch || fetch
  }
  async suggester(input) {
    const res = await this.fetch(`./internal/geofeature/suggestions?q=${input}`)
    const json = await res.json()
    return await json.filter(suggestion => !suggestion.id.startsWith('LITERAL'))
  }
  async geofeature(suggestion) {
    const { id } = suggestion
    const res = await this.fetch(`./internal/geofeature?id=${id}`)
    return await res.json()
  }
  async onChange(suggestion) {
    const geofeature =
      this.props.geofeature || (suggestItem => this.geofeature(suggestItem))
    this.setState({ value: suggestion.name, loading: true })
    try {
      const { type, geometry = {} } = await geofeature(suggestion)
      this.setState({ loading: false })

      switch (geometry.type) {
        case 'Polygon': {
          const polygon = geometry.coordinates[0]
          this.props.setState({
            hasKeyword: true,
            locationType: 'latlon',
            polygon,
            polyType: 'polygon',
            value: this.state.value,
          })
          break
        }
        case 'MultiPolygon': {
          const polygon = geometry.coordinates.map(function(ring) {
            return ring[0] // outer ring only
          })
          this.props.setState({
            hasKeyword: true,
            locationType: 'latlon',
            polygon,
            polyType: 'multipolygon',
            value: this.state.value,
          })
          break
        }
        default: {
          Announcement.announce({
            title: 'Invalid feature',
            message: 'Unrecognized feature type: ' + JSON.stringify(type),
            type: 'error',
          })
        }
      }
    } catch (e) {
      this.setState(
        { loading: false, error: 'Geo feature endpoint unavailable' },
        () => {
          if (typeof this.props.onError === 'function') {
            this.props.onError(e)
          }
        }
      )
    }
  }
  render() {
    const suggester = this.props.suggester || (input => this.suggester(input))
    const {
      polygon,
      cursor,
      polygonBufferWidth,
      polygonBufferUnits,
      polyType,
    } = this.props
    const { value, loading, error } = this.state
    return (
      <div>
        <AutoComplete
          value={value}
          onChange={option => this.onChange(option)}
          minimumInputLength={2}
          placeholder="Enter a region, country, or city"
          suggester={suggester}
        />
        {loading ? (
          <div style={{ marginTop: 10 }}>
            Loading geometry... <span className="fa fa-refresh fa-spin" />
          </div>
        ) : null}
        {!loading && error !== null ? <div>{error}</div> : null}
        {!loading && polygon !== undefined && polyType === 'polygon' ? (
          <Polygon
            polygon={polygon}
            cursor={cursor}
            polygonBufferWidth={polygonBufferWidth}
            polygonBufferUnits={polygonBufferUnits}
          />
        ) : null}
        {!loading && polygon !== undefined && polyType === 'multipolygon' ? (
          <MultiPolygon
            polygon={polygon}
            cursor={cursor}
            polygonBufferWidth={polygonBufferWidth}
            polygonBufferUnits={polygonBufferUnits}
          />
        ) : null}
      </div>
    )
  }
}

module.exports = Keyword
