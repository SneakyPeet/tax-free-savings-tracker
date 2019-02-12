.PHONY: all help dev clean build deploy

all: help

help:
	@echo "dev clean build deploy"

dev:
	clojure -m figwheel.main --build dev --repl

clean:
	rm -rf target

build:
	clojure -m figwheel.main -O advanced -bo dev

deploy:
	@echo "todo"
