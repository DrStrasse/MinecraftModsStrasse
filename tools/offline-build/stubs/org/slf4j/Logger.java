package org.slf4j;
public interface Logger {
    void info(String msg);
    void info(String format, Object... args);
    void warn(String msg);
    void error(String msg);
}
