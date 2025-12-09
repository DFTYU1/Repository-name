import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:qr_code_scanner/qr_code_scanner.dart';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'package:path_provider/path_provider.dart';
import 'package:vibration/vibration.dart';
import 'package:intl/intl.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:share_plus/share_plus.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Máy Quét Trùng Mã',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const HomePage(),
    );
  }
}

class DBHelper {
  static Database? _db;
  static const String tableName = 'scans';

  static Future<Database> init() async {
    if (_db != null) return _db!;
    Directory documentsDirectory = await getApplicationDocumentsDirectory();
    String path = join(documentsDirectory.path, 'scans.db');
    _db = await openDatabase(path, version: 1, onCreate: (db, v) async {
      await db.execute('''
      CREATE TABLE $tableName(
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        code TEXT UNIQUE,
        created_at TEXT
      );
      ''');
    });
    return _db!;
  }

  static Future<bool> exists(String code) async {
    final db = await init();
    final res = await db.query(tableName, where: 'code = ?', whereArgs: [code]);
    return res.isNotEmpty;
  }

  static Future<void> insert(String code) async {
    final db = await init();
    final now = DateFormat('yyyy-MM-dd HH:mm:ss').format(DateTime.now());
    await db.insert(tableName, {'code': code, 'created_at': now}, conflictAlgorithm: ConflictAlgorithm.ignore);
  }

  static Future<List<Map<String, dynamic>>> all() async {
    final db = await init();
    return await db.query(tableName, orderBy: 'created_at DESC');
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  String _last = '';
  int _count = 0;

  @override
  void initState() {
    super.initState();
    _loadCount();
  }

  Future<void> _loadCount() async {
    final rows = await DBHelper.all();
    setState(() {
      _count = rows.length;
    });
  }

  void _openScanner() async {
    final status = await Permission.camera.request();
    if (!status.isGranted) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Cần quyền truy cập camera')));
      }
      return;
    }
    final result = await Navigator.of(context).push<String>(
      MaterialPageRoute(builder: (_) => const ScannerPage()),
    );
    if (result != null) {
      _last = result;
      await _loadCount();
      setState(() {});
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Máy Quét Trùng Mã'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            ElevatedButton.icon(
              onPressed: _openScanner,
              icon: const Icon(Icons.qr_code_scanner),
              label: const Text('Mở máy quét'),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                const Text('Tổng số：'),
                Text('$_count', style: const TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(width: 16),
                const Text('Gần nhất：'),
                Expanded(child: Text(_last, overflow: TextOverflow.ellipsis)),
              ],
            ),
            const SizedBox(height: 16),
            const Divider(),
            const SizedBox(height: 8),
            const Text('Lịch sử quét', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Expanded(
              child: FutureBuilder<List<Map<String, dynamic>>>(
                future: DBHelper.all(),
                builder: (context, snapshot) {
                  if (!snapshot.hasData) return const Center(child: CircularProgressIndicator());
                  final rows = snapshot.data!;
                  if (rows.isEmpty) return const Center(child: Text('Chưa có dữ liệu'));
                  return ListView.builder(
                    itemCount: rows.length,
                    itemBuilder: (context, i) {
                      final r = rows[i];
                      return ListTile(
                        title: Text(r['code']),
                        subtitle: Text(r['created_at']),
                      );
                    },
                  );
                },
              ),
            )
          ],
        ),
      ),
    );
  }
}

class ScannerPage extends StatefulWidget {
  const ScannerPage({super.key});
  @override
  State<ScannerPage> createState() => _ScannerPageState();
}

class _ScannerPageState extends State<ScannerPage> {
  final GlobalKey qrKey = GlobalKey(debugLabel: 'QR');
  QRViewController? controller;
  bool _processing = false;

  void _onQRViewCreated(QRViewController controller) {
    this.controller = controller;
    controller.scannedDataStream.listen((scanData) async {
      if (_processing) return;
      _processing = true;

      final code = scanData.code?.trim() ?? '';
      if (code.isEmpty) {
        _processing = false;
        return;
      }

      final existed = await DBHelper.exists(code);

      if (existed) {
        if (await Vibration.hasVibrator() ?? false) {
          Vibration.vibrate(duration: 300);
        }

        if (mounted) {
          showDialog(
            context: context,
            builder: (_) => AlertDialog(
              title: const Text('Mã trùng'),
              content: Text('Đã tồn tại：\n$code'),
              actions: [
                TextButton(onPressed: () {
                  Navigator.of(context).pop();
                }, child: const Text('Xác nhận')),
              ],
            ),
          );
        }
      } else {
        await DBHelper.insert(code);

        if (await Vibration.hasVibrator() ?? false) {
          Vibration.vibrate(duration: 100);
        }

        if (mounted) {
          showDialog(
            context: context,
            builder: (_) => AlertDialog(
              title: const Text('Quét thành công'),
              content: Text('Đã lưu：\n$code'),
              actions: [
                TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Tiếp tục')),
              ],
            ),
          );
        }
      }

      await Future.delayed(const Duration(milliseconds: 500));
      _processing = false;
    });
  }

  @override
  void dispose() {
    controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Quét mã')),
      body: Column(
        children: [
          Expanded(
            flex: 5,
            child: QRView(
              key: qrKey,
              onQRViewCreated: _onQRViewCreated,
            ),
          ),
        ],
      ),
    );
  }
}
