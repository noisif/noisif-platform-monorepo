.DEFAULT_GOAL := no-argument

.PHONY: no-argument
no-argument:
	$(error error: running 'make' without an argument is forbidden)

.PHONY: init-hooks
init-hooks:
	-chmod +x .githooks/pre-commit
	-chmod +x .githooks/pre-push
	git config core.hooksPath .githooks

.PHONY: build
build:
	./gradlew build --rerun-tasks --continue

.PHONY: build-cache
build-cache:
	./gradlew build --continue

.PHONY: format-check
format-check:
	./gradlew :buildSrc:spotlessCheck spotlessCheck

.PHONY: format-apply
format-apply:
	./gradlew :buildSrc:spotlessApply spotlessApply

include Makefile.docker
include Makefile.ns
include Makefile.tools
