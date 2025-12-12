package com.example.qr_asset_tracker

import io.flutter.app.FlutterApplication
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.android.FlutterActivity

class Application : FlutterApplication() {

    override fun onCreate() {
        super.onCreate()

        // 初始化 Flutter 引擎（默认即可）
        val engine = FlutterEngine(this)
        engine.dartExecutor.executeDartEntrypoint(
            io.flutter.embedding.engine.dart.DartExecutor.DartEntrypoint.createDefault()
        )
        FlutterEngineCache.getInstance().put("my_engine", engine)
    }
}
