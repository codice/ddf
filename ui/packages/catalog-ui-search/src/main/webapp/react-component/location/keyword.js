const React = require('react')
const Announcement = require('component/announcement')

const AutoComplete = require('../auto-complete')
const Polygon = require('./polygon')

const fetch = require('js/fetch')

class Keyword extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      value: '',
      loading: false,
      error: null,
    }
    this.fetch = this.props.fetch || fetch
  }
  async onChange({ id, name }) {
    this.setState({ value: name, loading: true })
    try {
      const res = await this.fetch(`./internal/geofeature?id=${id}`)
      const { type, geometry = {} } = await res.json()
      this.setState({ loading: false })

      switch (geometry.type) {
        case 'Polygon': {
          const polygon = geometry.coordinates[0]
          this.props.setState({
            hasKeyword: true,
            locationType: 'latlon',
            polygon: polygon,
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
            polygon: polygon,
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
    const { polygon, cursor } = this.props
    const { value, loading, error } = this.state
    return (
      <div>
        <AutoComplete
          value={value}
          onChange={option => this.onChange(option)}
          minimumInputLength={2}
          placeholder="Enter a region, country, or city"
          url="./internal/geofeature/suggestions"
        />
        {loading ? (
          <div style={{ marginTop: 10 }}>
            Loading geometry... <span className="fa fa-refresh fa-spin" />
          </div>
        ) : null}
        {!loading && error !== null ? <div>{error}</div> : null}
        {!loading && polygon !== undefined ? (
          <Polygon polygon={polygon} cursor={cursor} />
        ) : null}
      </div>
    )
  }
}

module.exports = Keyword
