package local.aicenter.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
public final class XlsxReaderTest {
 private static int checks;
 private static void eq(Object a,Object b){checks++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 private static Map<String,String> fixture(){Map<String,String> p=new LinkedHashMap<>();
  p.put("xl/workbook.xml","<workbook xmlns='http://schemas.openxmlformats.org/spreadsheetml/2006/main' xmlns:r='http://schemas.openxmlformats.org/officeDocument/2006/relationships'><sheets><sheet name='中文' sheetId='1' r:id='r1'/><sheet name='Hidden' state='hidden' sheetId='2' r:id='r2'/></sheets></workbook>");
  p.put("xl/_rels/workbook.xml.rels","<Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'><Relationship Id='r1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet' Target='worksheets/sheet1.xml'/><Relationship Id='r2' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet' Target='/xl/worksheets/sheet2.xml'/></Relationships>");
  p.put("xl/sharedStrings.xml","<sst xmlns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'><si><r><t>中</t></r><r><t>文😀</t></r></si></sst>");
  p.put("xl/worksheets/sheet1.xml","<worksheet xmlns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'><sheetData><row r='1'><c r='A1' t='s'><v>0</v></c><c r='B1' t='inlineStr'><is><t>Hello</t></is></c><c r='C1'><f>1+1</f><v>2</v></c><c r='D1' t='e'><f>1/0</f><v>#DIV/0!</v></c><c r='E1'><f t='shared' si='0'/></c></row></sheetData></worksheet>");
  p.put("xl/worksheets/sheet2.xml","<worksheet xmlns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'><sheetData><row><c r='XFD1048576'><v>42</v></c></row></sheetData></worksheet>");return p;
 }
 private static byte[] zip(Map<String,String> parts)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(out)){for(Map.Entry<String,String> p:parts.entrySet()){z.putNextEntry(new ZipEntry(p.getKey()));z.write(p.getValue().getBytes(StandardCharsets.UTF_8));z.closeEntry();}}return out.toByteArray();}
 private static XlsxReader.Book read(Map<String,String> p)throws Exception{return XlsxReader.read(new ByteArrayInputStream(zip(p)),new StopController().begin());}
 private static void reject(Map<String,String> p)throws Exception{boolean bad=false;try{read(p);}catch(Exception expected){bad=true;}eq(bad,true);}
 public static void main(String[] args)throws Exception{
  Map<String,String> p=fixture();byte[] before=zip(p);XlsxReader.Book b=XlsxReader.read(new ByteArrayInputStream(before),new StopController().begin());
  eq(b.sheets.size(),2);eq(b.sheets.get(0).name,"中文");eq(b.sheets.get(1).state,"hidden");eq(b.sheets.get(0).cells.get(0).value,"中文😀");eq(b.sheets.get(0).cells.get(1).value,"Hello");eq(b.sheets.get(0).cells.get(2).formula,"1+1");eq(b.sheets.get(0).cells.get(2).value,"2");eq(b.sheets.get(0).cells.get(3).type,"e");eq(b.sheets.get(0).cells.get(4).formulaType,"shared");eq(b.sheets.get(0).cells.get(4).value,"");eq(b.preview().contains("3个公式，1个缓存错误"),true);eq(Arrays.equals(before,zip(p)),true);
  for(String unsafe:new String[]{"../escape","/absolute","xl/../bad","xl\\bad","http://bad"}){p=fixture();p.put(unsafe,"x");reject(p);}
  p=fixture();p.put("xl/sharedStrings.xml","<!DOCTYPE s [<!ENTITY secret SYSTEM 'file:///etc/passwd'>]><sst xmlns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'><si><t>&secret;</t></si></sst>");reject(p);
  p=fixture();p.put("xl/worksheets/sheet2.xml","<worksheet xmlns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'><c r='XFE1'><v>1</v></c></worksheet>");reject(p);
  p=fixture();p.put("xl/worksheets/sheet2.xml","<worksheet xmlns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'><c r='A1'/><c r='A1'/></worksheet>");reject(p);
  p=fixture();p.put("xl/worksheets/sheet2.xml","<worksheet xmlns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'><c r='A1' t='s'><v>999</v></c></worksheet>");reject(p);
  p=fixture();p.put("xl/vbaProject.bin","macro");reject(p);
  p=fixture();p.put("xl/_rels/workbook.xml.rels",p.get("xl/_rels/workbook.xml.rels").replace("Target='worksheets/sheet1.xml'","TargetMode='External' Target='https://example.invalid/sheet.xml'"));reject(p);
  p=fixture();p.put("large.xml","x".repeat(2*1024*1024+1));reject(p);
  p=fixture();p.remove("xl/workbook.xml");reject(p);
  p=fixture();p.put("xl/worksheets/sheet2.xml","<worksheet xmlns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'>"+"<x>".repeat(70)+"</x>".repeat(70)+"</worksheet>");reject(p);
  p=fixture();p.put("xl/worksheets/sheet2.xml","<worksheet xmlns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'><c r='A1' t='inlineStr'><is><t>"+"x".repeat(16385)+"</t></is></c></worksheet>");reject(p);
  p=fixture();p.put("xl/worksheets/sheet2.xml","<worksheet xmlns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'><c r='A1' t='inlineStr'><is><t>"+"x".repeat(300)+"</t></is></c></worksheet>");eq(read(p).preview().contains("(预览截短)"),true);
  p=fixture();p.put("xl/workbook.xml",p.get("xl/workbook.xml").replace("http://schemas.openxmlformats.org/spreadsheetml/2006/main","https://example.invalid/unsupported"));reject(p);
  StopController stop=new StopController();StopController.Token token=stop.begin();token.cancel();boolean cancelled=false;try{XlsxReader.read(new ByteArrayInputStream(before),token);}catch(StopController.Stopped e){cancelled=true;}eq(cancelled,true);
  System.out.println("PASS xlsx bounded read-only reader: "+checks+" assertions (synthetic files; host only)");
 }
}
