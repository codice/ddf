const { spawn } = require('child_process')
const phantomjs = require('phantomjs-prebuilt')

module.exports = ({ args, pkg }) => {
  const bin = require.resolve('mocha-phantomjs-core/mocha-phantomjs-core.js')

  spawn(phantomjs.path, [
    bin,
    args,
    'spec',
    JSON.stringify({
      hooks: require.resolve('mocha-phantomjs-istanbul'),
      coverageFile: 'target/coverage.json'
    })
  ], { stdio: 'inherit' })
}
