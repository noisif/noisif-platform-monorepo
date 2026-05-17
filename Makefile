.DEFAULT_GOAL := no-argument

.PHONY: no-argument
no-argument:
	$(error error: running 'make' without an argument is forbidden)

.PHONY: build
build:
	./gradlew build --rerun-tasks --continue

.PHONY: build-cache
build-cache:
	./gradlew build --continue


include Makefile.docker
include Makefile.jws
include Makefile.tools
