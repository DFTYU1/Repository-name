package local.aicenter.core;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
public final class XlsxEditorTest {
 private static int checks;
 private static void eq(Object a,Object b){checks++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 static byte[] fixture()throws Exception{Map<String,String> p=XlsxReaderTest.fixture();p.put("xl/worksheets/sheet1.xml",p.get("xl/worksheets/sheet1.xml").replace("<f t='shared' si='0'/>","<f>SUM(A1:A2)</f>").replace("<c r='A1'", "<c s='3' r='A1'"));p.put("docProps/custom.xml","<original>preserve &amp; retain</original>");p.put("xl/calcChain.xml","<calcChain/>");p.put("[Content_Types].xml","<Types xmlns='http://schemas.openxmlformats.org/package/2006/content-types'><Override PartName='/xl/calcChain.xml' ContentType='application/vnd.openxmlformats-officedocument.spreadsheetml.calcChain+xml'/></Types>");p.put("xl/_rels/workbook.xml.rels",p.get("xl/_rels/workbook.xml.rels").replace("</Relationships>","<Relationship Id='chain' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/calcChain' Target='calcChain.xml'/></Relationships>"));return XlsxReaderTest.zip(p);}
 private static byte[] edit(byte[] b,String kind,String value)throws Exception{return XlsxEditor.edit(b,"中文","A1",kind,value,new StopController().begin());}
 private static XlsxReader.Book book(byte[] b)throws Exception{return XlsxReader.read(new ByteArrayInputStream(b),new StopController().begin());}
 private static void reject(byte[] b,String sheet,String cell,String kind,String value)throws Exception{boolean rejected=false;try{XlsxEditor.edit(b,sheet,cell,kind,value,new StopController().begin());}catch(Exception e){rejected=true;}eq(rejected,true);}
 public static void main(String[] args)throws Exception{
  byte[] input=fixture(),before=input.clone(),result=edit(input,"text"," <&中文😀> ");eq(Arrays.equals(input,before),true);XlsxReader.Book b=book(result);eq(b.sheets.get(0).cells.get(0).value," <&中文😀> ");eq(b.sheets.get(0).cells.get(0).type,"inlineStr");eq(b.sheets.get(0).cells.get(2).formula,"1+1");eq(b.sheets.get(0).cells.get(2).value,"");eq(b.sheets.get(0).cells.get(3).value,"");eq(b.sheets.get(1).cells.get(0).value,"42");
  Map<String,byte[]> original=XlsxReader.packageParts(new ByteArrayInputStream(input),new StopController().begin()),parts=XlsxReader.packageParts(new ByteArrayInputStream(result),new StopController().begin());eq(Arrays.equals(original.get("docProps/custom.xml"),parts.get("docProps/custom.xml")),true);eq(parts.containsKey("xl/calcChain.xml"),false);eq(new String(parts.get("[Content_Types].xml"),StandardCharsets.UTF_8).contains("calcChain"),false);eq(new String(parts.get("xl/_rels/workbook.xml.rels"),StandardCharsets.UTF_8).contains("calcChain"),false);eq(new String(parts.get("xl/workbook.xml"),StandardCharsets.UTF_8).contains("fullCalcOnLoad=\"1\""),true);eq(new String(parts.get("xl/worksheets/sheet1.xml"),StandardCharsets.UTF_8).contains("s=\"3\""),true);
  eq(book(edit(input,"number","0.10000000000000000001")).sheets.get(0).cells.get(0).value,"0.10000000000000000001");eq(book(edit(input,"formula"," =SUM(B1:B3) ")).sheets.get(0).cells.get(0).formula,"SUM(B1:B3)");eq(book(edit(input,"formula","=SUM(B1:B3)")).sheets.get(0).cells.get(0).value,"");
  String injection="=HYPERLINK(\"https://example.invalid\")";eq(book(edit(input,"text",injection)).sheets.get(0).cells.get(0).formula,null);
  reject(input,"不存在","A1","text","x");reject(input,"中文","Z9","text","x");reject(input,"中文","A1","number","NaN");reject(input,"中文","A1","number","1e2147483647");reject(input,"中文","A1","formula",injection);reject(input,"中文","A1","formula","=SUMIFS(A1:A3,B1:B2,\"NG\")");reject(input,"中文","A1","bad","1");reject(input,"中文","A1","text","x".repeat(2049));
  reject(XlsxReaderTest.zip(XlsxReaderTest.fixture()),"中文","A1","text","x");
  Map<String,String> signed=XlsxReaderTest.fixture();signed.put("_xmlsignatures/sig1.xml","<sig/>");reject(XlsxReaderTest.zip(signed),"中文","A1","text","x");
  Map<String,String> protectedParts=new LinkedHashMap<>();for(Map.Entry<String,byte[]> e:original.entrySet())protectedParts.put(e.getKey(),new String(e.getValue(),StandardCharsets.UTF_8));protectedParts.put("xl/worksheets/sheet1.xml",protectedParts.get("xl/worksheets/sheet1.xml").replace("</worksheet>","<sheetProtection sheet='1'/></worksheet>"));reject(XlsxReaderTest.zip(protectedParts),"中文","A1","text","x");
  StopController.Token token=new StopController().begin();token.cancel();boolean stopped=false;try{XlsxEditor.edit(input,"中文","A1","text","x",token);}catch(StopController.Stopped e){stopped=true;}eq(stopped,true);
  System.out.println("PASS xlsx copy editor: "+checks+" assertions (host synthetic only; no formula recalculation claim)");
 }
}
