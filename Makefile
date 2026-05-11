.DEFAULT_GOAL := no-argument

.PHONY: no-argument
no-argument:
	$(error error: running 'make' without an argument is forbidden)

include Makefile.docker
include Makefile.jws
include Makefile.tools
