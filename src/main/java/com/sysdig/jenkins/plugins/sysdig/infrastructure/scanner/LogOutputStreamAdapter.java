package com.sysdig.jenkins.plugins.sysdig.infrastructure.scanner;

import com.sysdig.jenkins.plugins.sysdig.domain.SysdigLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

class LogOutputStreamAdapter extends OutputStream {
  private final SysdigLogger logger;
  private final StringBuilder buffer = new StringBuilder();
  private final String newline = System.lineSeparator();

  public LogOutputStreamAdapter(SysdigLogger logger) {
    this.logger = logger;
  }

  @Override
  public void write(int b) throws IOException {
    // Write one byte at a time to the buffer
    buffer.append((char) b);
    checkForNewline();
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    // Convert byte array to a string and accumulate in the buffer
    buffer.append(new String(b, off, len, StandardCharsets.UTF_8));
    checkForNewline();
  }

  private void checkForNewline() {
    int newlineIndex;
    while ((newlineIndex = buffer.indexOf(newline)) != -1) {
      // Process each line separated by the system newline
      String line = buffer.substring(0, newlineIndex);
      logger.logInfo(line);
      buffer.delete(0, newlineIndex + newline.length());
    }
  }

  @Override
  public void flush() throws IOException {
    // Call logInfo if there is any remaining message in the buffer
    if (buffer.length() > 0) {
      logger.logInfo(buffer.toString());
      buffer.setLength(0);
    }
  }

  @Override
  public void close() throws IOException {
    flush();
    super.close();
  }
}
