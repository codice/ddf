var getConfig = function (env) {
  console.log('build catalog-ui-search in ' + env + ' environment')
  switch (env) {
    case 'production': return require('./prod');
    case 'test': return require('./test');
    case 'ci': return require('./ci');
    case 'development':
    default: return require('./dev');
  }
}

module.exports = getConfig(process.env.NODE_ENV || 'development')
