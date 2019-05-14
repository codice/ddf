const properties = require('../../js/properties')
const api = require('./index')
const oldInit = properties.init

const mock = () => {
  properties.init = function() {
    const data = api('./internal/config')
    const uiConfig = api('./internal/platform/config/ui')
    // use this function to initialize variables that rely on others
    let props = this
    props = _.extend(props, data)
    props.ui = uiConfig
    this.handleEditing()
    this.handleFeedback()
    this.handleExperimental()
    this.handleUpload()
    this.handleListTemplates()
  }
  properties.init()
}

const unmock = () => {
  properties.init = oldInit
}

export { mock, unmock }
