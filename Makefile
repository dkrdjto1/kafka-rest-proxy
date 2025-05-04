TOOLS = wget git tar rm cp docker sha512sum
K := $(foreach exec,$(TOOLS),\
	$(if $(shell which $(exec)),tools exists,$(error "No $(exec) in PATH")))

DIRS = docker
BUILDDIRS = $(DIRS:%=build-%)
CLEANDIRS = $(DIRS:%=clean-%)

all: $(BUILDDIRS)
$(DIRS): $(BUILDDIRS)
$(BUILDDIRS):
	$(MAKE) -C $(@:build-%=%)

dist:
	$(MAKE) -C dist
	
clean: $(CLEANDIRS)
$(CLEANDIRS):
	$(MAKE) -C $(@:clean-%=%) clean

.PHONY: subdirs $(DIRS)
.PHONY: subdirs $(BUILDDIRS)
.PHONY: subdirs $(CLEANDIRS)
.PHONY: all clean
