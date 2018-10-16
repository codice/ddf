const React = require('react')
const Keyword = require('react-component/location/keyword.js')
const properties = require('properties')

const onlineGazetteer = properties.onlineGazetteer

class Gazetteer extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
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
  async geofeature(idParts) {
    const [type, id] = idParts.split(':')
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
            geofeature={id => this.geofeature(id)}
          />
        ) : (
          <Keyword setState={this.props.setState} />
        )}
      </div>
    )
  }
}

module.exports = Gazetteer
