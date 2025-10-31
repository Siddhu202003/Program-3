# Target to compile all Java files
compile: bin
	javac -cp antlr-4.13.2-complete.jar -d bin simplf/*.java simplf/parser/*.java

# Target to create bin directory if it doesn't exist
bin:
	mkdir -p bin

clean:
	rm -rf bin