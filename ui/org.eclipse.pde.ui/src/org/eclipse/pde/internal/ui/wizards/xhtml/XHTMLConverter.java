package org.eclipse.pde.internal.ui.wizards.xhtml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Stack;

import org.apache.lucene.demo.html.HTMLParser;
import org.apache.lucene.demo.html.HTMLParserConstants;
import org.apache.lucene.demo.html.Token;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public class XHTMLConverter {

	public static final int XHTML_STRICT = 0;
	public static final int XHTML_TRANSITIONAL = 1;
	public static final int XHTML_FRAMESET = 2;
	private static final String[] XHTML_DOCTYPES = new String[3];
	static {
		XHTML_DOCTYPES[XHTML_STRICT] = 
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"; //$NON-NLS-1$
		XHTML_DOCTYPES[XHTML_TRANSITIONAL] = 
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"; //$NON-NLS-1$
		XHTML_DOCTYPES[XHTML_FRAMESET] =
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Frameset//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd\">"; //$NON-NLS-1$
	}
	private static final String XHTML_DEFAULT_DOCTYPE = XHTML_DOCTYPES[XHTML_TRANSITIONAL];
	private static final String XMLNS = "xmlns"; //$NON-NLS-1$
	private static final String XMLNS_LOC = "http://www.w3.org/1999/xhtml"; //$NON-NLS-1$
	private static final String F_XHTML_FE = "xhtml"; //$NON-NLS-1$
	
	private int fDoctype;
	
	public XHTMLConverter(int docType) {
		fDoctype = docType;
	}
	
	public void setType(int docType) {
		fDoctype = docType;
	}
	
	public void convert(IFile htmlIFile, IProgressMonitor monitor) throws CoreException {
		File htmlFile = new File(htmlIFile.getLocation().toString());
		if (!htmlFile.exists())
			// file not found, possibly a duplicate href entry
			// (in this case it has already been converted)
			return;
		
		StringWriter writer = new StringWriter();
		PrintWriter pwriter = new PrintWriter(writer);
		write(htmlFile, pwriter);
		monitor.worked(1);
		pwriter.flush();
		pwriter.close();
		
		writer.flush();
		try {
			modifyFile(htmlIFile, writer, monitor);
			writer.close();
		} catch (IOException e) {
		}
	}
	
	private void write(File file, PrintWriter pw) {
		try {
			Stack tagStack = new Stack();
			HTMLParser parser = new HTMLParser(file);
			pw.println(getDoctypeString(fDoctype));
			XHTMLTag htmlTag = grabNextTag(parser, "<html"); //$NON-NLS-1$
			htmlTag.addAttribute(XMLNS, XMLNS_LOC);
			// fill in any remaning attributes the html tag had
			convertTagContents(parser, htmlTag);
			htmlTag.write(pw);
			tagStack.push(htmlTag);
			
			Token token = parser.getNextToken();
			while (isValid(token)) {
				switch (token.kind) {
				case HTMLParserConstants.TagName:
					XHTMLTag tag = new XHTMLTag(token.image, fDoctype);
					convertTagContents(parser, tag);
					if (tag.isClosingTag()) {
						// Closinsg tag encountered:
						// - pop a tag from the stack and close it
						if (tagStack.isEmpty())
							break;
						XHTMLTag topStack = (XHTMLTag)tagStack.pop();
						topStack.writeClosed(pw);
						break;
					}
					if (!tag.isEmptyTag())
						// Non-empty tags get pushed on the stack for closing
						tagStack.push(tag);
					
					tag.write(pw);
					break;
				default:
					pw.print(token.image);
				}
				token = parser.getNextToken();
			}
			
			// close all remaining tags
			while (!tagStack.isEmpty()) {
				XHTMLTag topStack = (XHTMLTag)tagStack.pop();
				topStack.writeClosed(pw);
			}
		} catch (FileNotFoundException e) {
		}
	}
	
	public String prepareXHTMLFileName(String filename) {
		int period = filename.lastIndexOf("."); //$NON-NLS-1$
		if (period > -1)
			return filename.substring(0, period + 1) + F_XHTML_FE;
		return filename + F_XHTML_FE;
	}
	
	private void modifyFile(IFile htmlFile, StringWriter writer, IProgressMonitor monitor) throws CoreException, IOException {
		// set new contents
		ByteArrayInputStream bais = new ByteArrayInputStream(writer.toString().getBytes());
		htmlFile.setContents(bais, IResource.KEEP_HISTORY | IResource.FORCE, monitor);
		bais.close();
		monitor.worked(1);
		// rename to .xhtml
		IPath newPath = htmlFile.getFullPath().removeFileExtension().addFileExtension(F_XHTML_FE);
		htmlFile.move(newPath, IResource.KEEP_HISTORY | IResource.FORCE, monitor);
		monitor.worked(1);
	}
	
	private XHTMLTag grabNextTag(HTMLParser parser, String tag) {
		Token token = parser.getNextToken();
		while (isValid(token)) {
			if (token.kind == HTMLParserConstants.TagName
					&& token.image.equalsIgnoreCase(tag))
				return new XHTMLTag(token.image, fDoctype);
			token = parser.getNextToken();
		}
		return null;
	}
	
	private void convertTagContents(HTMLParser parser, XHTMLTag tag) {
		if (tag == null)
			return;
		Token token = parser.getNextToken();
		while (isValid(token) && token.kind != HTMLParserConstants.TagEnd) {
			tag.eatToken(token);
			token = parser.getNextToken();
		}
		tag.expandLeftoverAttribute();
		// last token read is either invalid or a TagEnd - we don't care about either
	}
	
	private boolean isValid(Token token) {
		return token != null && token.kind != HTMLParserConstants.EOF;
	}
	
	private String getDoctypeString(int version) {
		if (version != XHTML_FRAMESET &&
				version != XHTML_STRICT &&
				version != XHTML_FRAMESET)
			return XHTML_DEFAULT_DOCTYPE;
		return XHTML_DOCTYPES[version];
	}
}
