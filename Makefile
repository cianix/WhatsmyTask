all:
	rm -fr *class
	javac -cp whatsmytask.jar:. *java
	zip -u whatsmytask.jar *.class
	rm *class
