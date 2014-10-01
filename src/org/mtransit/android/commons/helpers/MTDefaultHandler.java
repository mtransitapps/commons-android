package org.mtransit.android.commons.helpers;

import java.io.IOException;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTDefaultHandler extends DefaultHandler implements MTLog.Loggable {

	public MTDefaultHandler() {
		super();
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "%s()", getLogTag());
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "characters(%s,%s,%s)", ch, start, length);
		}
		super.characters(ch, start, length);
	}

	@Override
	public void endDocument() throws SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "endDocument()");
		}
		super.endDocument();
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "endElement(%s,%s,%s)", uri, localName, qName);
		}
		super.endElement(uri, localName, qName);
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "endPrefixMapping(%s)", prefix);
		}
		super.endPrefixMapping(prefix);
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "error(%s)", e);
		}
		super.error(e);
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "fatalError(%s)", e);
		}
		super.fatalError(e);
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "ignorableWhitespace(%s,%s,%s)", ch, start, length);
		}
		super.ignorableWhitespace(ch, start, length);
	}

	@Override
	public void notationDecl(String name, String publicId, String systemId) throws SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "notationDecl(%s,%s,%s)", name, publicId, systemId);
		}
		super.notationDecl(name, publicId, systemId);
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "processingInstruction(%s,%s)", target, data);
		}
		super.processingInstruction(target, data);
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "resolveEntity(%s,%s)", publicId, systemId);
		}
		return super.resolveEntity(publicId, systemId);
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "setDocumentLocator(%s)", locator);
		}
		super.setDocumentLocator(locator);
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "skippedEntity(%s)", name);
		}
		super.skippedEntity(name);
	}

	@Override
	public void startDocument() throws SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "startDocument()");
		}
		super.startDocument();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "startElement(%s,%s,%s,%s)", uri, localName, qName, attributes);
		}
		super.startElement(uri, localName, qName, attributes);
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "startPrefixMapping(%s,%s)", prefix, uri);
		}
		super.startPrefixMapping(prefix, uri);
	}

	@Override
	public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "unparsedEntityDecl(%s,%s,%s,%s)", name, publicId, systemId, notationName);
		}
		super.unparsedEntityDecl(name, publicId, systemId, notationName);
	}

	@Override
	public void warning(SAXParseException e) throws SAXException {
		if (Constants.LOG_DATA_PARSING) {
			MTLog.v(this, "warning(%s)", e);
		}
		super.warning(e);
	}

}
