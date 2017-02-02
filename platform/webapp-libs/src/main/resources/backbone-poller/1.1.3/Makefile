all: npm minify.code test

npm:
	@echo "`date`\tUpdating node modules"
	@npm install
	@npm update

minify.code:
	@echo "`date`\tMinifying javascript"
	@grunt uglify

test.lint:
	@echo "`date`\tRunning a javascript linter"
	@grunt lint

test.unit:
	@echo "`date`\tRunning unit tests"
	@grunt jasmine

test: test.lint test.unit

docs:
	@echo "`date`\tCreating annotated source code"
	@git checkout gh-pages
	@git merge --no-edit master
	@grunt docco
	@mv docs/backbone.poller.html index.html
	@mv docs/docco.css .
	@rm -rf docs
	@git add index.html docco.css
	@git commit -m "Updated annotated soucre code" --allow-empty
	@git push
	@git checkout master
