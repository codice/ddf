const glob = require('glob')
const path = require('path')

const flatten = (l, v) => l.concat(v)

module.exports = (dirs) => {
  return dirs
    .filter((dir) => !dir.match('target'))
    .map((d) => path.resolve(d))
    .map((d) => glob.sync(d + '/package.json'))
    .reduce(flatten, [])
    .map((d) => {
      const json = require(d)
      json.__path = d
      return json
    })
}
