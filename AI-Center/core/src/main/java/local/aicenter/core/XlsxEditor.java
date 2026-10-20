package local.aicenter.core;

import java.io.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** Modify one existing cell in a newly generated package; never writes the input. No formula evaluation. */
public final class XlsxEditor {
 private XlsxEditor(){}
 private static final String NS="http://schemas.openxmlformats.org/spreadsheetml/2006/main";
 public static byte[] edit(byte[] original,String sheetName,String address,String kind,String value,StopController.Token token)throws Exception{
  if(original==null||original.length>XlsxReader.MAX_FILE||value==null||value.length()>2048)throw new IllegalArgumentException("文件或单元格内容超限");
  XlsxReader.Book book=XlsxReader.read(new ByteArrayInputStream(original),token);XlsxReader.Sheet selected=null;
  for(XlsxReader.Sheet s:book.sheets){if(s.name.equals(sheetName))selected=s;for(XlsxReader.Cell c:s.cells)if(c.formula!=null&&!"normal".equals(c.formulaType))throw new IllegalArgumentException("暂不修改含共享、数组或数据表公式的工作簿");}
  if(selected==null)throw new IllegalArgumentException("工作表不存在");boolean found=false;for(XlsxReader.Cell c:selected.cells)if(c.address.equals(address))found=true;if(!found)throw new IllegalArgumentException("当前只能修改已存单元格");
  if(!Arrays.asList("text","number","formula").contains(kind))throw new IllegalArgumentException("未知单元格类型");
  if("number".equals(kind)){BigDecimal n=new BigDecimal(value.trim());if(Math.abs((long)n.scale())>1000||n.precision()>1000)throw new IllegalArgumentException("数值精度或指数超限");value=n.toPlainString();if(value.length()>2048)throw new IllegalArgumentException("数值过长");}
  if("formula".equals(kind))value=value.trim();
  if("formula".equals(kind)&&!FormulaCheck.check(value).startsWith("STRUCTURE_ONLY:"))throw new IllegalArgumentException("暂仅允许结构检查通过的单层公式；不计算结果");
  Map<String,byte[]> parts=XlsxReader.packageParts(new ByteArrayInputStream(original),token);
  for(String path:parts.keySet())if(path.startsWith("_xmlsignatures/")||path.toLowerCase(Locale.ROOT).contains("externallink"))throw new IllegalArgumentException("暂不修改签名或外链工作簿");
  for(XlsxReader.Sheet s:book.sheets){token.check();Document doc=parse(parts.get(s.partName));NodeList cells=doc.getElementsByTagNameNS(NS,"c");boolean changed=false;
   for(int i=0;i<cells.getLength();i++){Element cell=(Element)cells.item(i);boolean target=s==selected&&address.equals(cell.getAttribute("r"));boolean formula=direct(cell,"f")!=null;
    if(formula){remove(cell,"v");changed=true;}
    if(target){remove(cell,"v");remove(cell,"f");remove(cell,"is");cell.removeAttribute("t");
     if("text".equals(kind)){cell.setAttribute("t","inlineStr");Element is=doc.createElementNS(NS,"is"),text=doc.createElementNS(NS,"t");text.setAttributeNS("http://www.w3.org/XML/1998/namespace","xml:space","preserve");text.setTextContent(value);is.appendChild(text);cell.appendChild(is);}
     else{Element content=doc.createElementNS(NS,"formula".equals(kind)?"f":"v");content.setTextContent("formula".equals(kind)?value.substring(1):value);cell.appendChild(content);}changed=true;
    }
   }
   if(changed)parts.put(s.partName,serialize(doc));
  }
  Document workbook=parse(parts.get("xl/workbook.xml"));Element root=workbook.getDocumentElement(),calc=direct(root,"calcPr");if(calc==null){calc=workbook.createElementNS(NS,"calcPr");Node next=null;for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling())if(n instanceof Element&&Arrays.asList("oleSize","customWorkbookViews","pivotCaches","smartTagPr","smartTagTypes","webPublishing","fileRecoveryPr","webPublishObjects","extLst").contains(n.getLocalName())){next=n;break;}root.insertBefore(calc,next);}calc.setAttribute("calcMode","auto");calc.setAttribute("fullCalcOnLoad","1");calc.setAttribute("forceFullCalc","1");parts.put("xl/workbook.xml",serialize(workbook));
  parts.remove("xl/calcChain.xml");
  for(String path:new String[]{"xl/_rels/workbook.xml.rels","[Content_Types].xml"})if(parts.containsKey(path)){
   Document metadata=parse(parts.get(path));NodeList children=metadata.getDocumentElement().getChildNodes();
   for(int i=children.getLength()-1;i>=0;i--){Node n=children.item(i);if(n instanceof Element){Element e=(Element)n;if(e.getAttribute("Type").endsWith("/calcChain")||"/xl/calcChain.xml".equals(e.getAttribute("PartName")))e.getParentNode().removeChild(e);}}
   parts.put(path,serialize(metadata));
  }
  ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(out)){for(Map.Entry<String,byte[]> part:parts.entrySet()){token.check();zip.putNextEntry(new ZipEntry(part.getKey()));zip.write(part.getValue());zip.closeEntry();}}
  byte[] result=out.toByteArray();if(result.length>XlsxReader.MAX_FILE)throw new IOException("修改副本超过上限");
  XlsxReader.Book verified=XlsxReader.read(new ByteArrayInputStream(result),token);boolean valid=false;for(XlsxReader.Sheet s:verified.sheets)if(s.name.equals(sheetName))for(XlsxReader.Cell c:s.cells)if(c.address.equals(address))valid="formula".equals(kind)?value.substring(1).equals(c.formula):value.equals(c.value)&&c.formula==null;if(!valid)throw new IOException("副本复核失败");token.check();return result;
 }
 private static Element direct(Element parent,String local){for(Node n=parent.getFirstChild();n!=null;n=n.getNextSibling())if(n instanceof Element&&NS.equals(n.getNamespaceURI())&&local.equals(n.getLocalName()))return (Element)n;return null;}
 private static void remove(Element parent,String local){Element e;while((e=direct(parent,local))!=null)parent.removeChild(e);}
 private static Document parse(byte[] bytes)throws Exception{
  if(bytes==null)throw new IOException("缺少工作簿部件");String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();String upper=text.toUpperCase(Locale.ROOT);if(upper.contains("<!DOCTYPE")||upper.contains("<!ENTITY")||text.indexOf('\0')>=0)throw new IOException("禁止外部实体");
  javax.xml.parsers.SAXParserFactory sf=javax.xml.parsers.SAXParserFactory.newInstance();sf.setNamespaceAware(true);org.xml.sax.XMLReader guard=sf.newSAXParser().getXMLReader();guard.setContentHandler(new org.xml.sax.helpers.DefaultHandler(){int depth;@Override public void startElement(String u,String l,String q,org.xml.sax.Attributes a)throws SAXException{if(++depth>64)throw new SAXException("XML depth exceeded");}@Override public void endElement(String u,String l,String q){depth--;}});guard.setEntityResolver((id,url)->{throw new SAXException("External entity forbidden");});guard.parse(new InputSource(new StringReader(text)));
  DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setExpandEntityReferences(false);for(String feature:new String[]{"http://xml.org/sax/features/external-general-entities","http://xml.org/sax/features/external-parameter-entities","http://apache.org/xml/features/nonvalidating/load-external-dtd"})try{f.setFeature(feature,false);}catch(javax.xml.parsers.ParserConfigurationException ignored){/* Guard above forbids declarations. */}
  javax.xml.parsers.DocumentBuilder b=f.newDocumentBuilder();b.setEntityResolver((id,url)->{throw new SAXException("External entity forbidden");});return b.parse(new InputSource(new StringReader(text)));
 }
 private static byte[] serialize(Document doc)throws Exception{Transformer transformer=TransformerFactory.newInstance().newTransformer();transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");transformer.setOutputProperty(OutputKeys.INDENT,"no");ByteArrayOutputStream out=new ByteArrayOutputStream();transformer.transform(new DOMSource(doc),new StreamResult(out));if(out.size()>2*1024*1024)throw new IOException("XML部件超限");return out.toByteArray();}
}
