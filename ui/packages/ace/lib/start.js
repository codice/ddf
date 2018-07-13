const open = require('opn')
const webpack = require('webpack')
const WebpackDevServer = require('webpack-dev-server')
const chalk = require('chalk')

const webpackConfig = require('./webpack.config')

module.exports = ({ args, pkg }) => {
  const publicPath = pkg['context-path']
  const port = process.env.PORT || args.port || 8080
  const host = process.env.HOST || args.host || 'localhost'

  const config = webpackConfig({
    env: process.env.NODE_ENV || args.env || 'development',
    main: pkg.main,
    alias: pkg.alias,
    publicPath,
    auth: args.auth ||
      console.log(chalk.yellow('WARNING: using default basic auth (admin:admin)! See options for how to override this.')) ||
      'admin:admin'
  })

  const devServer = { host, port, ...config.devServer }

  WebpackDevServer.addDevServerEntrypoints(config, devServer)
  const compiler = webpack(config)
  const server = new WebpackDevServer(compiler, devServer)

  server.listen(port, host, (err) => {
    if (err) {
      console.error(err)
      process.exit(1)
    } else if (args.open) {
      open(`http://${host}:${port}${publicPath}/`)
    }
  })
}
