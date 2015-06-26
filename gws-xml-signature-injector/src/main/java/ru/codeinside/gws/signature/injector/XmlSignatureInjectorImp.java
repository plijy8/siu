/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright (c) 2015, MPL CodeInside http://codeinside.ru
 */

package ru.codeinside.gws.signature.injector;


import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ru.codeinside.gws.api.Signature;
import ru.codeinside.gws.api.WrappedAppData;
import ru.codeinside.gws.api.XmlSignatureInjector;
import ru.codeinside.gws.api.XmlTypes;

import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.cert.CertificateEncodingException;
import java.util.logging.Logger;

public final class XmlSignatureInjectorImp implements XmlSignatureInjector {
  private static final String APP_DATA = "AppData";
  private static final String ACTOR_SMEV = "http://smev.gosuslugi.ru/actors/smev";
  private static final String WSSE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
  private static final String WSS_X509V3 = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3";
  private static final String WSS_BASE64_BINARY = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary";
  private static final String SignatureSpecNS = "http://www.w3.org/2000/09/xmldsig#";

  final Logger log = Logger.getLogger(XmlSignatureInjector.class.getName());

  @Override
  public String injectSpToAppData(WrappedAppData wrappedAppData) {
    DocumentBuilder documentBuilder = getDocumentBuilder();
    Document document = parseData(wrappedAppData.getWrappedAppData(), documentBuilder);

    validateAppData(document);

    Element signatureElement = assembleSignature(wrappedAppData.getSignature(), getIdAttr(document).getValue());
    insertSignatureToAppData(document, signatureElement);
    return contentToString(document);
  }

  /**
   * Встроить ЭП-ОВ в заголовок SOAP сообщения
   *
   * @param message
   * @param signature
   * @return
   */
  @Override
  public void injectOvToSoapHeader(SOAPMessage message, Signature signature) {
    final SOAPPart doc = message.getSOAPPart();

    try {

      SOAPElement security = (SOAPElement) message.getSOAPHeader().getChildElements().next();
      SOAPElement signatureElement = (SOAPElement) security.getChildElements().next();

      buildBinarySecurityToken(doc, security, signatureElement, signature);

      NodeList list = signatureElement.getChildNodes();
      Element keyInfo = (Element) list.item(list.getLength() - 1);

      addSignedValueElement(doc, signatureElement, keyInfo, signature);

    } catch (SOAPException e) {
      e.printStackTrace();
      throw new IllegalStateException("Ошибка встраивания подписи СП-ОВ в SOAP-сообщеие", e);
    } catch (CertificateEncodingException e) {
      e.printStackTrace();
      throw new IllegalStateException("Ошибка встраивания подписи СП-ОВ в SOAP-сообщеие", e);
    }

  }

  @Override
  public void prepareSoapMessage(SOAPMessage message, byte[] bodyHash) {
    final SOAPPart doc = message.getSOAPPart();

    try {

      Signature signature = new Signature(null, null, null, bodyHash, true);

      final SOAPElement security = buildSecurityElement(message);
      QName wsuId = doc.getEnvelope().createQName("Id", "wsu");

      final String bodyId = message.getSOAPBody().getAttributeValue(wsuId);
      final XMLDSign xmldSign = new XMLDSign(signature, bodyId);

      addSignatureElement(doc, security, xmldSign);

    } catch (SOAPException e) {
      e.printStackTrace();
      throw new IllegalStateException("Ошибка формирования заголовка SOAP-сообщения", e);
    }
  }

  private Attr getIdAttr(Document document) {
    Attr attrId = document.getDocumentElement().getAttributeNode("Id");
    if (attrId == null || attrId.getValue().isEmpty()) {
      throw new IllegalStateException("AppData must have 'Id' attribute");
    }
    return attrId;
  }

  private void validateAppData(Document document) {
    String localName = document.getDocumentElement().getLocalName();
    if (!APP_DATA.equals(localName)) {
      throw new IllegalStateException("Expected 'AppData' tag, but was: " + localName);
    }
  }

  private String contentToString(Document document) {
    StreamResult result = new StreamResult(new StringWriter());
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.transform(new DOMSource(document), result);
      return result.getWriter().toString();
    } catch (TransformerConfigurationException e) {
      throw new RuntimeException(e);
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
  }

  private void insertSignatureToAppData(Document document, Element signatureElement) {
    Node imported = document.importNode(signatureElement, true);
    Element documentElement = document.getDocumentElement();
    documentElement.insertBefore(imported, documentElement.getFirstChild());
  }

  private Element assembleSignature(Signature signature, String id) {
    XMLDSign xmldSign = new XMLDSign(signature, id);
    xmldSign.setEnveloped(true);
    return XmlTypes.beanToElement(xmldSign, true);
  }

  private Document parseData(String source, DocumentBuilder documentBuilder) {
    InputSource is = new InputSource(new StringReader(source));
    try {
      return documentBuilder.parse(is);
    } catch (SAXException e) {
      log.severe("Unable to parse appData to Document: " + e.getMessage());
      throw new RuntimeException(e);
    } catch (IOException e) {
      log.severe("IOException when parsing appData: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  private DocumentBuilder getDocumentBuilder() {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setIgnoringElementContentWhitespace(true);
    documentBuilderFactory.setNamespaceAware(true);
    documentBuilderFactory.setCoalescing(true);
    try {
      return documentBuilderFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      log.severe("Unable to get DocumentBuilder: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  private SOAPElement buildSecurityElement(SOAPMessage message) throws SOAPException {
    final SOAPHeader header = message.getSOAPHeader();
    final QName actor = header.createQName("actor", header.getPrefix());
    final SOAPElement security = header.addChildElement("Security", "wsse", WSSE);
    security.addAttribute(actor, ACTOR_SMEV);

    return security;
  }

  private void buildBinarySecurityToken(SOAPPart doc, SOAPElement security, Element signatureElement, Signature signature)
      throws SOAPException, CertificateEncodingException {

    Element binarySecurityToken = doc.createElementNS(WSSE, "BinarySecurityToken");
    binarySecurityToken.setPrefix(security.getPrefix());
    binarySecurityToken.setAttribute("EncodingType", WSS_BASE64_BINARY);
    binarySecurityToken.setAttribute("ValueType", WSS_X509V3);
    binarySecurityToken.setTextContent(DatatypeConverter.printBase64Binary(signature.certificate.getEncoded()));
    binarySecurityToken.setAttribute("wsu:Id", "CertId");
    security.insertBefore(binarySecurityToken, signatureElement);
  }

  private void addSignatureElement(SOAPPart doc, SOAPElement security, XMLDSign xmldSign) {
    Element signatureElement = XmlTypes.beanToElement(xmldSign, true);
    Element element = (Element) doc.importNode(signatureElement, true);

    Element keyInfo = doc.createElementNS(SignatureSpecNS, "KeyInfo");
    Element securityTokenReference = doc.createElementNS(WSSE, "SecurityTokenReference");
    Element reference = doc.createElementNS(WSSE, "Reference");
    reference.setAttribute("URI", "#CertId");
    reference.setAttribute("ValueType", WSS_X509V3);
    securityTokenReference.appendChild(reference);
    keyInfo.appendChild(securityTokenReference);
    element.appendChild(keyInfo);
    security.appendChild(element);
  }

  private void addSignedValueElement(SOAPPart doc, SOAPElement signatureElement, Element keyInfo, Signature signature) throws SOAPException {
    Element signatureValue = doc.createElementNS(SignatureSpecNS, "SignatureValue");
    signatureValue.setPrefix(signatureElement.getPrefix());
    signatureValue.setTextContent(DatatypeConverter.printBase64Binary(signature.sign));
    signatureElement.insertBefore(signatureValue, keyInfo);
  }
}
