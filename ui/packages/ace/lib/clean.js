const path = require('path')
const rimraf = require('rimraf')
const glob = require('glob')

const flatten = (l, v) => l.concat(v)

const getTargets = (pkg, onlyWorkspaces) => {
  const targets = onlyWorkspaces ? [] : [path.resolve('target')]

  if (!pkg.workspaces) {
    return targets
  }

  const workspaces = pkg.workspaces
    .map(d => path.resolve(d))
    .map(d => glob.sync(d))
    .reduce(flatten, [])
    .map(d => path.join(d, 'target'))

  return targets.concat(workspaces)
}

module.exports = ({ args, pkg }) => {
  getTargets(pkg, args.workspaces).forEach(directory => {
    rimraf.sync(directory)
    console.log('ace info removed ' + directory)
  })
}
