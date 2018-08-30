const webpack = require('webpack')

const config = require('./webpack.config')

module.exports = ({ args, pkg }) => {
  const c = config({
    env: process.env.NODE_ENV || args.env || 'development',
    main: pkg.main,
    alias: pkg.alias,
  })

  webpack(c, (err, stats) => {
    if (err || stats.hasErrors()) {
      console.log(err || stats.toString())
      process.exit(1)
    }
  })
}
