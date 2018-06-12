const CLIEngine = require('eslint').CLIEngine

module.exports = ({ args, pkg }) => {
  var results = new CLIEngine().executeOnFiles(['./'])
  results.results = results.results.filter(function (result) {
    delete result.source
    return result.errorCount
  })
  if (results.errorCount) {
    throw new Error(JSON.stringify(results, null, 2))
  }
}
