CC=gcc
CFLAGS=-shared -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -I$(HEADER_PATH) -fPIC

ifndef JAVA_HOME
$(error JAVA_HOME is not set)
endif

lib: $(C_PATH)/Wrapper.c
	$(CC) $(CFLAGS) -o $(LIB_OUT) $(C_PATH)/Wrapper.c
