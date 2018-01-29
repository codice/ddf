const fs = require('fs')
const path = require('path')

const ls = require('./ls-packages')

const bundle = ({ groupId, artifactId, version, packaging, classifier }) =>
  `<bundle>mvn:${groupId}/${artifactId}/${version}/${packaging}/${classifier}</bundle>`

const feature = ({ project, packages }) => `<features name="${project.artifactId}-${project.version}"
          xmlns="http://karaf.apache.org/xmlns/features/v1.3.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://karaf.apache.org/xmlns/features/v1.3.0 http://karaf.apache.org/xmlns/features/v1.3.0">
    <feature name="${project.artifactId}" install="auto" version="${project.version}">
        <bundle>mvn:org.eclipse.jetty/jetty-servlets/9.2.19.v20160908</bundle>
        <bundle>mvn:${project.groupId}/${project.artifactId}/${project.version}</bundle>
        ${packages.map(bundle).join('\n        ')}
    </feature>
</features>
`
const extract = ($, selectors) => Object.keys(selectors).reduce((o, key) => {
  o[key] = $(selectors[key]).text()
  return o
}, {})

const info = {
  groupId: 'project > groupId',
  artifactId: 'project > artifactId',
  version: 'project > parent > version'
}

module.exports = ({ args, pkg, pom }) => {
  const project = extract(pom, info)

  const packages = ls(pkg.workspaces)
    .filter((pkg) => pkg['context-path'] !== undefined)

  const config = {
    project,
    packages: packages.map(({ name }) => ({
      groupId: project.groupId,
      artifactId: project.artifactId,
      packaging: 'jar',
      classifier: name,
      version: project.version
    }))
  }

  const absolute = path.resolve('target/features.xml')

  fs.writeFile(absolute, feature(config), 'utf8', (err) => {
    if (err) throw err
    console.log(`wrote ${absolute}`)
  })
}
