const { text } = require('../../react-component/storybook')
import { KILOMETERS, METERS, MILES, NAUTICAL_MILES, YARDS } from '../geometry'

const lengthUnitKnob = () =>
  text(
    `coordinate Unit (${KILOMETERS}/${METERS}/${MILES}/${NAUTICAL_MILES}/${YARDS})`,
    KILOMETERS
  )

export default lengthUnitKnob
