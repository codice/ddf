const serialize = time => {
  if (!time || time.unit === undefined || time.last === undefined) {
    return
  }
  const prefix = time.unit === 'm' || time.unit === 'h' ? 'PT' : 'P'
  return `RELATIVE(${prefix + time.last + time.unit.toUpperCase()})`
}
const deserialize = value => {
  if (!value) {
    return
  }

  const match = value.match(/RELATIVE\(Z?([A-Z]*)(\d+\.*\d*)(.)\)/)
  if (!match) {
    return
  }

  let [, prefix, last, unit] = match
  last = parseFloat(last)
  unit = unit.toLowerCase()
  if (prefix === 'P' && unit === 'm') {
    //must capitalize months
    unit = unit.toUpperCase()
  }

  return {
    last,
    unit,
  }
}
export { serialize, deserialize }
