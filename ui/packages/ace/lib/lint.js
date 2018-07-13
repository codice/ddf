const CLIEngine = require('eslint').CLIEngine
const cheerio = require('cheerio')
const glob = require('glob')
const fs = require('fs')

module.exports = ({ args, pkg }) => {
  var htmlResults = { errorCount: 0, errors: [] }
  console.log(process.cwd())
  var files = glob.sync('packages/*/src/**/{*.html,*.handlebars}', { matchBase: true, realpath: true })
  console.log('files: ' + files.length)
  files.forEach(file => {
    try {
      var reportHtml = function (file, element, attribute, value) {
        if (!(value.startsWith('.') || value.startsWith('#')) && !(value.startsWith('javascript') || value.startsWith('{'))) {
          htmlResults.errors.push({ 'file': file, 'element': element, 'attribute': attribute, 'value': value })
          htmlResults.errorCount++
        }
      }
      var html = cheerio.load(
        fs.readFileSync(file, { encoding: 'utf8' }),
        { xmlMode: true, decodeEntities: false })
      html('link').each((i, elem) => {
        if (elem.attribs.href) {
          reportHtml(file, 'link', 'href', elem.attribs.href)
        }
      })
      html('a').each((i, elem) => {
        if (elem.attribs.href) {
          reportHtml(file, 'a', 'href', elem.attribs.href)
        }
      })
      html('script').each((i, elem) => {
        if (elem.attribs.src) {
          reportHtml(file, 'script', 'src', elem.attribs.src)
        }
      })
    } catch (e) {
      console.error(e)
    }
  })
  var jsResults = new CLIEngine().executeOnFiles(['./'])
  jsResults.results = jsResults.results.filter(function (result) {
    delete result.source
    return result.errorCount
  })
  if (jsResults.errorCount || htmlResults.errorCount) {
    throw new Error('Non-relative paths found' + '\n' + JSON.stringify(jsResults, null, 2) + '\n' + JSON.stringify(htmlResults, null, 2))
  }
}
