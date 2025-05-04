
DOCKER = docker

## version, target
VERSION ?= $(shell cat ../VERSION)
TARGET := $(shell cat ../TARGET)

## build arg
BUILD_ARGS := --no-cache --force-rm 

.PHONY: all clean 

all: docker

copy-source-file:
	rm -rf src && mkdir src && rsync -av --exclude='docker' --exclude='raw' ../ src/

clean: dockerclean
	rm -rf dist load_docker-image.sh $(TARGET)-docker.tar src

dockerclean:
	$(DOCKER) rmi -f "$(TARGET):latest"
	$(DOCKER) rmi -f "$(TARGET):$(VERSION)"
	
docker: copy-source-file
	DOCKER_BUILDKIT=1 $(DOCKER) build \
	$(BUILD_ARGS) \
	-t "$(TARGET):latest" \
	-t "$(TARGET):$(VERSION)" \
	./

$(TARGET)-docker.tar: docker
	docker save $(TARGET):$(VERSION) > $(TARGET)-docker.tar

dist: $(TARGET)-docker.tar
	mkdir -p dist
	TARGET=$(TARGET) VERSION=$(VERSION) envsubst '$${TARGET},$${VERSION}' < load_docker-image.tmpl > load_docker-image.sh
	chmod a+x load_docker-image.sh
	tar zcf dist/$(TARGET)-docker_$(VERSION).tar.gz \
	--transform 's,^,$(TARGET)-docker_$(VERSION)/,' \
	$(TARGET)-docker.tar load_docker-image.sh examples
