
# Tools

GRW	:= ./gradlew

JFLAGS	:= -sourcepath $(SOURCE_DIR) -d $(OUTPUT_DIR) -cp lib/antlr-4.7-complete.jar 


all:
	$(GRW) shadowJar

clean:
	$(GRW) clean

test:
	$(GRW) check

.PHONY: all clean


