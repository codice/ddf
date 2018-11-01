const openBrowser = require('react-dev-utils/openBrowser')
const webpack = require('webpack')
const WebpackDevServer = require('webpack-dev-server')
const chalk = require('chalk')

const webpackConfig = require('./webpack.config')

module.exports = ({ args, pkg }) => {
  const publicPath = pkg['context-path']

  const env = process.env.NODE_ENV || args.env || 'development'
  const port = process.env.PORT || args.port || 8080
  const host = process.env.HOST || args.host || 'localhost'
  const proxy = process.env.PROXY || args.proxy || 'https://localhost:8993'

  const config = webpackConfig({
    env,
    proxy,
    publicPath,
    auth: args.auth ||
      console.log(chalk.yellow('WARNING: using default basic auth (admin:admin)! See options for how to override this.')) ||
      'admin:admin',
    main: pkg.main,
    alias: pkg.alias
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
      openBrowser(`http://${host}:${port}${publicPath}/`)
    }
  })
}
