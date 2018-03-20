const fs = require('fs')
const path = require('path')

const concatLess = (dir, name, ext = '.less') => {
  const filePath = path.join(dir, name + ext)
  const file = fs.readFileSync(filePath, { encoding: 'utf-8' })
  return file.replace(/@import\s*("|')([^"']+)("|')/g, (match, _, importPath) => {
    if (importPath.match(/\.css$/)) return ''
    const dirname = path.dirname(importPath)
    const basename = path.basename(importPath).replace(/\.less$/, '')
    return concatLess(path.join(dir, dirname), basename)
  })
}

module.exports = function (source, map) {
  this.cacheable()
  const { dir, name, ext } = path.parse(this.resourcePath)
  const less = concatLess(dir, name, ext)
  this.callback(null, less, map)
}
