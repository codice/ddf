const { text } = require('../../react-component/storybook')
import { LAT_LON, LAT_LON_DMS, USNG, UTM } from '../coordinate-editor'

const coordinateUnitKnob = () =>
  text(
    `coordinate Unit ('${LAT_LON}' / '${LAT_LON_DMS}' / '${USNG}' / '${UTM}')`,
    LAT_LON
  )

export default coordinateUnitKnob
