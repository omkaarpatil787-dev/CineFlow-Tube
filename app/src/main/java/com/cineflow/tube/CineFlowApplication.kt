package com.cineflow.tube

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.cineflow.tube.data.repository.CineFlowDownloadManager

class CineFlowApplication : Application(), ImageLoaderFactory {

    /** App-scoped download manager – single instance shared via Application reference */
    lateinit var downloadManager: CineFlowDownloadManager
        private set

    override fun onCreate() {
        super.onCreate()
        downloadManager = CineFlowDownloadManager(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("cineflow_image_cache"))
                    .maxSizeBytes(128L * 1024L * 1024L) // 128 MB disk cache
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}
