const fs = require('fs')

const { runner } = require('mocha-headless-chrome')

module.exports = ({ args, pkg }) => {
  runner({ file: args, reporter: 'spec', args: ['no-sandbox'] })
    .then(({ result, coverage }) => {
      if (result.stats.failures) {
        throw 'Tests failed'
      }
      fs.writeFileSync('target/coverage.json', JSON.stringify(coverage) || '')
    })
    .catch(err => {
      console.error(err)
      process.exit(1)
    })
}
