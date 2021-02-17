const user = require('../singletons/user-instance.js')


module.exports = {
    getEnumValue(model) {
    const multivalued = model.get('property').get('enumMulti')
    let value = model.get('value')
    if (value !== undefined && model.get('property').get('type') === 'DATE') {
      if (multivalued && value.map) {
        value = value.map(subvalue => user.getUserReadableDateTime(subvalue))
      } else {
        value = user.getUserReadableDateTime(value)
      }
    }
    if (!multivalued) {
      value = [value]
    }
    return value
  }}