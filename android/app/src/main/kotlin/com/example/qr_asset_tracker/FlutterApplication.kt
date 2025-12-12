package com.example.qr_asset_tracker

import io.flutter.app.FlutterApplication
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.FlutterEngineCache

class FlutterApplication : FlutterApplication() {
    override fun onCreate() {
        super.onCreate()

        // 初始化 Flutter 引擎（适配部分旧插件）
        val engine = FlutterEngine(this)
        engine.dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint.createDefault()
        )

        FlutterEngineCache
            .getInstance()
            .put("my_engine_id", engine)
    }
}
