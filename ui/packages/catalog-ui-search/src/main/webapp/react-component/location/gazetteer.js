const React = require('react')
const Keyword = require('react-component/location/keyword.js')
const properties = require('properties')

import fetch from '../../react-component/utils/fetch'

const onlineGazetteer = properties.onlineGazetteer

class Gazetteer extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
    this.fetch = this.props.fetch || fetch
  }
  expandPoint(geo) {
    const offset = 0.1
    if (geo.length === 1) {
      const point = geo[0]
      return [
        {
          lat: point.lat + offset,
          lon: point.lon + offset,
        },
        {
          lat: point.lat + offset,
          lon: point.lon - offset,
        },
        {
          lat: point.lat - offset,
          lon: point.lon - offset,
        },
        {
          lat: point.lat - offset,
          lon: point.lon + offset,
        },
      ]
    }
    return geo
  }
  extractGeo(suggestion) {
    return {
      type: 'Feature',
      geometry: {
        type: 'Polygon',
        coordinates: [
          this.expandPoint(suggestion.geo).map(coord => [coord.lon, coord.lat]),
        ],
      },
      properties: {},
      id: suggestion.id,
    }
  }
  async suggesterWithLiteralSupport(input) {
    const res = await this.fetch(`./internal/geofeature/suggestions?q=${input}`)
    return await res.json()
  }
  async geofeatureWithLiteralSupport(suggestion) {
    if (suggestion.id === 'LITERAL') {
      return this.extractGeo(suggestion)
    }
    const { id } = suggestion
    const res = await this.fetch(`./internal/geofeature?id=${id}`)
    return await res.json()
  }
  getOsmTypeSymbol(type) {
    switch (type) {
      case 'node':
        return 'N'
      case 'way':
        return 'W'
      case 'relation':
        return 'R'
      default:
        throw 'Unexpected OSM type ' + type
    }
  }
  async suggester(input) {
    const res = await window.fetch(
      `https://nominatim.openstreetmap.org/search?format=json&q=${input}`
    )
    const suggestions = await res.json()
    return suggestions.map(place => {
      return {
        id: this.getOsmTypeSymbol(place.osm_type) + ':' + place.osm_id,
        name: place.display_name,
      }
    })
  }
  async geofeature(suggestion) {
    const [type, id] = suggestion.id.split(':')
    const res = await window.fetch(
      `https://nominatim.openstreetmap.org/reverse?format=json&osm_type=${type}&osm_id=${id}`
    )
    const data = await res.json()
    return {
      type: 'Feature',
      geometry: {
        type: 'Polygon',
        coordinates: [
          [
            [data.boundingbox[2], data.boundingbox[0]],
            [data.boundingbox[3], data.boundingbox[0]],
            [data.boundingbox[2], data.boundingbox[1]],
            [data.boundingbox[3], data.boundingbox[1]],
          ],
        ],
      },
      properties: {},
      id: data.display_name,
    }
  }
  render() {
    return (
      <div>
        {onlineGazetteer ? (
          <Keyword
            setState={this.props.setState}
            suggester={input => this.suggester(input)}
            geofeature={suggestItem => this.geofeature(suggestItem)}
          />
        ) : (
          <Keyword
            setState={this.props.setState}
            suggester={input => this.suggesterWithLiteralSupport(input)}
            geofeature={suggestItem =>
              this.geofeatureWithLiteralSupport(suggestItem)
            }
          />
        )}
      </div>
    )
  }
}

module.exports = Gazetteer
