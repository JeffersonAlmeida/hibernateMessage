//$Id: ReaderInputStream.java,v 1.1 2005/02/12 03:09:21 oneovthafew Exp $
package org.hibernate.lob;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Exposes a <tt>Reader</tt> as an <tt>InputStream</tt>
 * @author Gavin King
 */
public class ReaderInputStream extends InputStream {
	
	private Reader reader;
	
	public ReaderInputStream(Reader reader) {
		this.reader = reader;
	}
	
	public int read() throws IOException {
		return reader.read();
	}
	
}
