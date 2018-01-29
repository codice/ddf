#!/usr/bin/env node

const fs = require('fs')
const program = require('commander')

const find = require('find-up')
const cheerio = require('cheerio')

const wrap = (cmd) => (args) => {
  let pkg, pom

  try {
    pkg = require(find.sync('package.json'))
    pom = cheerio.load(
      fs.readFileSync(find.sync('pom.xml'), {encoding: 'utf8'}),
      { xmlMode: true, decodeEntities: false })
  } catch (e) {
    console.error(e)
  }

  cmd({ args, pkg, pom })
}

const pkg = require('./package.json')

program.version(pkg.version)
program.description(pkg.description)

program
  .command('package')
  .description('build a jar')
  .action(wrap(require('./lib/package')))

program
  .command('set-env [args...]')
  .description('run args with `ACE_BUILD` environment variable set')
  .action(wrap(require('./lib/set-env')))

program
  .command('install [jar]')
  .description('install a jar into ~/.m2')
  .action(wrap(require('./lib/install')))

program
  .command('clean')
  .description('remove target directory')
  .action(wrap(require('./lib/clean')))

program
  .command('test [html]')
  .description('run mocha tests in a headless browser')
  .action(wrap(require('./lib/test')))

program
  .command('pom')
  .description('verify/fix the root pom')
  .option('--fix', 'sync pom with packages')
  .action(wrap(require('./lib/pom')))

program
  .command('gen-feature [path]')
  .description('generate a feature file')
  .action(wrap(require('./lib/gen-feature')))

program.parse(process.argv)

