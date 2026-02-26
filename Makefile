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
