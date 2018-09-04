const fs = require('fs')
const path = require('path')
const beautify = require('vkbeautify')

const ls = require('./ls-packages')

const missingArtifacts = (packages, $) => {
  const attached = $('artifacts > artifact')
    .map((i, el) =>
      $(el)
        .find('classifier')
        .text()
    )
    .get()

  return packages.filter(({ name }) => !attached.includes(name)).length > 0
}

const jarPath = (name, __path) => {
  const packagePath = path.dirname(__path)
  const jarPath = path.join(packagePath, 'target', name + '.jar')
  return path.relative(path.resolve('.'), jarPath)
}

const bin = () => require.resolve('ace/bin.js')

const includeArtifacts = (missing, $) => {
  const h = (tag, ...children) => $('<' + tag + '>').append(children)

  const artifacts = h(
    'artifacts',
    [
      `<!-- NOTE: please don't edit these artifacts manually. -->`,
      `<![CDATA[ They were calculated by running "ace pom --fix". ]]>`,
      `<!-- Please re-run the command if the artifacts need to be updated. -->`,
      h(
        'artifact',
        h('file', 'target/features.xml'),
        h('type', 'xml'),
        h('classifier', 'features')
      ),
    ].concat(
      missing.map(({ name, __path }) =>
        h(
          'artifact',
          h('file', jarPath(name, __path)),
          h('type', 'jar'),
          h('classifier', name)
        )
      )
    )
  )

  $('artifacts').replaceWith(artifacts)
}

module.exports = ({ args, pkg, pom }) => {
  const packages = ls(pkg.workspaces).filter(
    pkg => pkg['context-path'] !== undefined
  )

  if (missingArtifacts(packages, pom) && !args.fix) {
    console.error('ace error pom.xml is out of sync with packages')
    console.error(`ace error please run '${bin()} pom --fix' to re-sync`)
    process.exit(1)
  }

  if (args.fix) {
    includeArtifacts(packages, pom)
    const xml = beautify.xml(pom.html())
    const absolute = path.resolve('pom.xml')
    fs.writeFileSync(absolute, xml, 'utf8')
    console.log(`wrote ${absolute}`)
  }
}
