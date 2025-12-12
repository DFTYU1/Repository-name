package com.example.qr_asset_tracker

import io.flutter.app.FlutterApplication
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache

class Application : FlutterApplication() {
    override fun onCreate() {
        super.onCreate()

        // 初始化 Flutter 引擎（可选，但推荐）
        val engine = FlutterEngine(this)
        FlutterEngineCache
            .getInstance()
            .put("my_engine_id", engine)
    }
}
