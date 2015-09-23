package org.wtf.skybar.source;

import javax.servlet.ServletOutputStream;
import java.io.OutputStream;

/**
 *
 */
public interface Source {

    void write(OutputStream outputStream);
}
