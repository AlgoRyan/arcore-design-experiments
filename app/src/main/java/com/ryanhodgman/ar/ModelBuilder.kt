package com.ryanhodgman.ar

import android.content.Context
import android.net.Uri
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Responsible for constructing [ModelRenderable]s.
 *
 * @author Ryan Hodgman
 */
class ModelBuilder {

    companion object {

        /**
         * Builds the [ModelRenderable] from the specified asset. Note that the asset must exist in
         * the `/asset' folder and be a '.sfb' file.
         *
         * @param context An Android context with which to access the asset file.
         * @param assetName The name of the asset to be built, ending with '.sfb'.
         */
        suspend fun build(context: Context, assetName: String): ModelRenderable =
                suspendCoroutine { continuation ->
                    ModelRenderable.builder()
                            .setSource(context, Uri.parse(assetName))
                            .build()
                            .thenAccept { continuation.resume(it) }
                            .exceptionally {
                                continuation.resumeWithException(it)
                                null
                            }
                }
    }
}