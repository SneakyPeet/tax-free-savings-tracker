.PHONY: all help dev clean build deploy

all: help

help:
	@echo "dev clean build deploy"

dev:
	clojure -m figwheel.main --build dev --repl

devcards:
	clojure -m figwheel.main --build devcards --repl

clean:
	rm -rf target

build:
	clojure -m figwheel.main -O advanced -bo dev

deploy:
	make clean
	rm -rf docs
	mkdir docs
	mkdir docs/cljs-out
	mkdir docs/cljs-out/dev
	make build
	cp resources/public/index.html resources/public/favicon.ico docs
	cp target/public/cljs-out/dev-main.js target/public/cljs-out/dev-vendor.js docs/cljs-out
	cp target/public/cljs-out/dev/cljs_base.js docs/cljs-out/dev
