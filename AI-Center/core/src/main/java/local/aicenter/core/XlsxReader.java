package local.aicenter.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/** Bounded read-only SpreadsheetML preview. Never evaluates formulas or extracts ZIP paths. */
public final class XlsxReader {
    private XlsxReader() {}
    public static final int MAX_FILE=10*1024*1024;
    private static final int MAX_XML=2*1024*1024, MAX_EXPANDED=20*1024*1024;
    public static final class Cell {
        public final String address,type,value,formula,formulaType;
        Cell(String a,String t,String v,String f,String ft){address=a;type=t;value=v;formula=f;formulaType=ft;}
    }
    public static final class Sheet {
        public final String name,state;
        public final List<Cell> cells=new ArrayList<>();
        Sheet(String n,String s){name=n;state=s;}
    }
    public static final class Book {
        public final List<Sheet> sheets=new ArrayList<>();
        public String preview(){
            StringBuilder out=new StringBuilder("Excel临时只读预览（不保存、不上传）\n公式未重新计算；缓存可能过期。日期/格式未解释，数字按原始值显示；不支持图表、宏、样式或修复。\n");
            int shown=0,formulas=0,errors=0,total=0;
            for(Sheet s:sheets){out.append("\n工作表：").append(s.name).append(" [").append(s.state).append("]\n");
                for(Cell c:s.cells){total++;if(c.formula!=null)formulas++;if("e".equals(c.type))errors++;
                    if(shown++<100){out.append(c.address).append("：");if(c.formula!=null)out.append("=").append(shortText(c.formula)).append(" [公式类型:").append(c.formulaType).append("；未计算] 缓存：");out.append(c.value.isEmpty()?"(空/无缓存)":shortText(c.value)).append("e".equals(c.type)?" [缓存错误]":"").append('\n');}}
            }
            return out.append("\n共").append(sheets.size()).append("表，").append(total).append("个已存单元格，").append(formulas).append("个公式，").append(errors).append("个缓存错误；最多预览100格。共享/数组公式不展开。合并格只显示已存单元格。\n").toString();
        }
    }
    private static String shortText(String text){return text.length()>200?text.substring(0,200)+"…(预览截短)":text;}
    private static IOException invalid(){return new IOException("不支持、损坏或超出读取上限的xlsx文件");}
    public static Book read(InputStream input,StopController.Token token)throws Exception{
        byte[] compressed=bounded(input,MAX_FILE,token);Map<String,byte[]> parts=new LinkedHashMap<>();int expanded=0,entries=0;
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(compressed))){ZipEntry entry;
            while((entry=zip.getNextEntry())!=null){token.check();String name=entry.getName();if(++entries>200||!safe(name)||parts.containsKey(name))throw invalid();
                byte[] bytes=bounded(zip,MAX_XML,token);expanded+=bytes.length;if(expanded>MAX_EXPANDED)throw invalid();parts.put(name,bytes);
                if(name.endsWith("vbaProject.bin")||name.contains("/embeddings/"))throw invalid();
            }
        }
        Map<String,String> relations=new LinkedHashMap<>();
        parse(required(parts,"xl/_rels/workbook.xml.rels"),"Relationships",new DefaultHandler(){
            @Override public void startElement(String u,String l,String q,Attributes a)throws SAXException{
                token.check();if(!"Relationship".equals(l))return;String id=a.getValue("Id"),target=a.getValue("Target"),type=a.getValue("Type");
                if(type!=null&&type.endsWith("/worksheet")){if("External".equals(a.getValue("TargetMode"))||id==null||target==null||relations.containsKey(id))throw new SAXException("Invalid worksheet relation");
                    String path=target.startsWith("/xl/")?target.substring(1):"xl/"+target;if(!safe(path)||!path.startsWith("xl/worksheets/"))throw new SAXException("Invalid part path");relations.put(id,path);}
            }
        },token);
        List<String> strings=new ArrayList<>();
        if(parts.containsKey("xl/sharedStrings.xml"))parse(parts.get("xl/sharedStrings.xml"),"sst",new DefaultHandler(){
            StringBuilder value;boolean text;
            @Override public void startElement(String u,String l,String q,Attributes a){token.check();if("si".equals(l))value=new StringBuilder();if("t".equals(l)&&value!=null)text=true;}
            @Override public void characters(char[] c,int s,int n)throws SAXException{if(text){value.append(c,s,n);if(value.length()>16384)throw new SAXException("Long string");}}
            @Override public void endElement(String u,String l,String q)throws SAXException{if("t".equals(l))text=false;if("si".equals(l)){if(strings.size()>=5000)throw new SAXException("Many strings");strings.add(value.toString());value=null;}}
        },token);
        Book book=new Book();List<String> paths=new ArrayList<>();
        parse(required(parts,"xl/workbook.xml"),"workbook",new DefaultHandler(){
            @Override public void startElement(String u,String l,String q,Attributes a)throws SAXException{token.check();if(!"sheet".equals(l))return;
                String id=a.getValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships","id");String path=relations.get(id),name=a.getValue("name"),state=a.getValue("state");
                if(path==null||name==null||name.length()>128||book.sheets.size()>=32)throw new SAXException("Invalid sheet");book.sheets.add(new Sheet(name,state==null?"visible":state));paths.add(path);
            }
        },token);
        if(book.sheets.isEmpty())throw invalid();int[] count={0};
        for(int i=0;i<paths.size();i++){Sheet sheet=book.sheets.get(i);
            parse(required(parts,paths.get(i)),"worksheet",new DefaultHandler(){
                String address,type,formulaType;StringBuilder value,formula;String capture="";boolean inCell;final java.util.HashSet<String> seen=new java.util.HashSet<>();
                @Override public void startElement(String u,String l,String q,Attributes a)throws SAXException{token.check();
                    if("c".equals(l)){if(inCell||++count[0]>5000)throw new SAXException("Many cells");inCell=true;address=a.getValue("r");type=a.getValue("t");if(type==null)type="n";if(!addressValid(address)||!seen.add(address))throw new SAXException("Invalid cell");value=new StringBuilder();formula=null;formulaType="normal";}
                    if(inCell&&("v".equals(l)||"t".equals(l)||"f".equals(l))){capture=l;if("f".equals(l)){formula=new StringBuilder();String ft=a.getValue("t");if(ft!=null){if(ft.length()>32)throw new SAXException("Invalid formula type");formulaType=ft;}}}
                }
                @Override public void characters(char[] c,int s,int n)throws SAXException{token.check();if(capture.isEmpty())return;StringBuilder b="f".equals(capture)?formula:value;b.append(c,s,n);if(b.length()>16384)throw new SAXException("Long cell");}
                @Override public void endElement(String u,String l,String q)throws SAXException{if(l.equals(capture))capture="";if("c".equals(l)&&inCell){String v=value.toString();if("s".equals(type)){try{int ix=Integer.parseInt(v);v=strings.get(ix);}catch(RuntimeException e){throw new SAXException("Invalid shared string index");}}sheet.cells.add(new Cell(address,type,v,formula==null?null:formula.toString(),formulaType));inCell=false;}}
            },token);
        }
        token.check();return book;
    }
    private static boolean addressValid(String address){if(address==null||!address.matches("[A-Z]{1,3}[1-9][0-9]{0,6}"))return false;int p=0,col=0;while(p<address.length()&&Character.isLetter(address.charAt(p)))col=col*26+address.charAt(p++)-'A'+1;return col<=16384&&Integer.parseInt(address.substring(p))<=1048576;}
    private static boolean safe(String name){if(name==null||name.isEmpty()||name.startsWith("/")||name.contains("\\")||name.contains(":")||name.contains("%")||name.contains("?")||name.contains("#"))return false;for(String p:name.split("/"))if(p.equals("..")||p.equals("."))return false;return true;}
    private static byte[] required(Map<String,byte[]> parts,String name)throws IOException{byte[] b=parts.get(name);if(b==null)throw invalid();return b;}
    private static byte[] bounded(InputStream in,int limit,StopController.Token token)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1){token.check();if(out.size()>limit-n)throw invalid();out.write(block,0,n);}return out.toByteArray();}
    private static void parse(byte[] bytes,String root,DefaultHandler handler,StopController.Token token)throws Exception{
        token.check();String xml=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        String upper=xml.toUpperCase(java.util.Locale.ROOT);if(upper.contains("<!DOCTYPE")||upper.contains("<!ENTITY")||xml.indexOf('\0')>=0)throw invalid();
        SAXParserFactory factory=SAXParserFactory.newInstance();factory.setNamespaceAware(true);factory.setValidating(false);
        XMLReader reader=factory.newSAXParser().getXMLReader();
        for(String feature:new String[]{"http://xml.org/sax/features/external-general-entities","http://xml.org/sax/features/external-parameter-entities","http://apache.org/xml/features/nonvalidating/load-external-dtd"}){try{reader.setFeature(feature,false);}catch(SAXException ignored){/* UTF-8 input guard rejects all DTD/entity declarations. */}}
        reader.setEntityResolver((publicId,systemId)->{throw new SAXException("External entities forbidden");});reader.setContentHandler(new DefaultHandler(){
            private int depth;
            @Override public void startElement(String u,String l,String q,Attributes a)throws SAXException{token.check();if(++depth>64)throw new SAXException("Deep XML");if(depth==1&&(!root.equals(l)||!("Relationships".equals(root)?"http://schemas.openxmlformats.org/package/2006/relationships":"http://schemas.openxmlformats.org/spreadsheetml/2006/main").equals(u)))throw new SAXException("Unsupported XML root/namespace");for(int i=0;i<a.getLength();i++)if(a.getValue(i).length()>16384)throw new SAXException("Long attribute");handler.startElement(u,l,q,a);}
            @Override public void characters(char[] c,int start,int length)throws SAXException{token.check();handler.characters(c,start,length);}
            @Override public void endElement(String u,String l,String q)throws SAXException{handler.endElement(u,l,q);depth--;}
        });reader.setErrorHandler(handler);
        reader.parse(new InputSource(new java.io.StringReader(xml)));token.check();
    }
}
