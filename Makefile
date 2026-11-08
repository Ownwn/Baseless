MAIN=com.ownwn.baseless.BaselessApplicationKt
SRC_DIR=src/main/kotlin

JAVA_SOURCES = $(shell find $(SRC_DIR) -name "*.java")
KOTLIN_SOURCES = $(shell find $(SRC_DIR) -name "*.kt")

all: compile run

compile:
	mkdir -p out

	javac -d out $(JAVA_SOURCES)
	kotlinc -cp out -d out $(KOTLIN_SOURCES)

jar: compile
	jar cfe app.jar $(MAIN) -C out .

run: jar
	cd frontend && npm run build && npm run deploy &
	kotlin -cp app.jar $(MAIN)

pull:
	curl -O -fsSL 'https://nightly.link/Ownwn/Baseless/workflows/makefile/main/app.zip'

	unzip -o app.zip
	rm -f app.zip

.PHONY: test

# HTTP checks use in-memory clients; curl checks start an isolated fixture server.
test: compile
	mkdir -p out/test
	javac -cp out -d out/test $$(find src/test/java -name '*.java')
	java -cp out:out/test com.ownwn.server.HttpRegressionTests
	python3 src/test/curl_http_tests.py
