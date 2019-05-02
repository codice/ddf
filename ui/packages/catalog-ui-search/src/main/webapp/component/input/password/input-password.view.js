const InputView = require('../input.view')
import * as React from 'react'
import InputPassword from './input-password'

module.exports = InputView.extend({
  template(props) {
    return <InputPassword />
  },
})
