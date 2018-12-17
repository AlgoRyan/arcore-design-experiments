package com.ryanhodgman.ar.nodes

import android.content.Context
import com.google.ar.core.PointCloud
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.rendering.RenderableDefinition.Submesh
import com.ryanhodgman.ar.Feature
import com.ryanhodgman.ar.features
import com.ryanhodgman.ar.numFeatures
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Renders an ARCore point cloud.
 *
 * Indicates the confidence of each feature point through the colour of that feature point, on a scale from light red
 * to dark red as the confidence increases.
 *
 * [Original implementation][https://github.com/claywilkinson/arcore-android-sdk/blob/sceneform-samples/samples/cloud_anchor_java/app/src/main/java/com/google/ar/core/examples/java/cloudanchor/sceneform/PointCloudNode.java]
 * by [Clayton Wilkinson][https://github.com/claywilkinson]
 *
 * @author Ryan Hodgman, with inspiration from Clayton Wilkinson
 */
class PointCloudNode : Node() {

    private companion object {
        private val colourConf0to20 = Color(1f, 1f, 0.8f)
        private val colourConf20to40 = Color(1f, 0.8f, 0.6f)
        private val colourConf40to60 = Color(1f, 0.6f, 0.4f)
        private val colourConf60to80 = Color(1f, 0.4f, 0.2f)
        private val colourConf80to100 = Color(1f, 0.2f, 0f)
    }

    private var materialConf0to20: Material? = null
    private var materialConf20to40: Material? = null
    private var materialConf40to60: Material? = null
    private var materialConf60to80: Material? = null
    private var materialConf80to100: Material? = null
    private var timestamp: Long = 0

    suspend fun prepare(context: Context) {
        materialConf0to20 = suspendCoroutine { continuation ->
            MaterialFactory.makeOpaqueWithColor(context, colourConf0to20)
                    .thenAccept { continuation.resume(it) }
        }
        materialConf20to40 = suspendCoroutine { continuation ->
            MaterialFactory.makeOpaqueWithColor(context, colourConf20to40)
                    .thenAccept { continuation.resume(it) }
        }
        materialConf40to60 = suspendCoroutine { continuation ->
            MaterialFactory.makeOpaqueWithColor(context, colourConf40to60)
                    .thenAccept { continuation.resume(it) }
        }
        materialConf60to80 = suspendCoroutine { continuation ->
            MaterialFactory.makeOpaqueWithColor(context, colourConf60to80)
                    .thenAccept { continuation.resume(it) }
        }
        materialConf80to100 = suspendCoroutine { continuation ->
            MaterialFactory.makeOpaqueWithColor(context, colourConf80to100)
                    .thenAccept { continuation.resume(it) }
        }
    }

    /**
     * Update the renderable for the point cloud. This creates a small quad for each feature point.
     */
    fun update(cloud: PointCloud) {
        // Skip if disabled, if the cloud has not been updated, or if the materials haven't yet been loaded
        if (!isEnabled || timestamp == cloud.timestamp || materialConf0to20 == null || materialConf20to40 == null
                || materialConf40to60 == null || materialConf60to80 == null || materialConf80to100 == null) return
        timestamp = cloud.timestamp
        if (cloud.numFeatures < 1) {
            // No features in the cloud to be rendered
            renderable = null
            return
        }
        // Create pyramid submeshes that match each feature point
        val vertices = mutableListOf<Vertex>()
        val submeshes = mutableListOf<Submesh>()
        cloud.features.forEachIndexed { featureIndex, feature ->
            val mesh = PyramidMesh(feature)
            vertices.addAll(mesh.vertices)
            // Determines the material colour based upon the feature point confidence
            val material = when (feature.confidence) {
                in 0.0..0.2 -> materialConf0to20
                in 0.2..0.4 -> materialConf20to40
                in 0.4..0.6 -> materialConf40to60
                in 0.6..0.8 -> materialConf60to80
                else -> materialConf80to100
            }
            submeshes.add(Submesh.builder()
                    .setName("Feature $featureIndex")
                    .setMaterial(material)
                    .setTriangleIndices(mesh.indices.map { meshIndex ->
                        // Each index matches to a vertex, and there are four vertices per feature
                        featureIndex * 4 + meshIndex
                    })
                    .build())
        }
        // Create Renderable for the given point cloud
        val renderableDef = RenderableDefinition.builder()
                .setVertices(vertices)
                .setSubmeshes(submeshes)
                .build()
        ModelRenderable.builder()
                .setSource(renderableDef)
                .build()
                .thenAccept { renderable ->
                    renderable.isShadowCaster = false
                    setRenderable(renderable)
                }
    }

    private class PyramidMesh(feature: Feature) {

        private companion object {
            private const val TRIANGLE_SIZE = 0.005f

            private val uv0 = Vertex.UvCoordinate(0f, 0f)
            private val normalTop = Vector3(0f, 0f, 1f)
            private val normalLeft = Vector3(.7f, 0f, .7f)
            private val normalFront = Vector3(-.7f, 0f, .7f)
            private val normalRight = Vector3(0f, 1f, 0f)
        }

        // The indices of the triangle faces need to be listed counter clockwise as
        // appears when facing the front side.
        val indices = listOf(
                // Left
                1, 2, 0,
                // Right
                0, 2, 3,
                // Back
                0, 3, 1,
                // Bottom
                1, 2, 3
        )

        val vertices = listOf<Vertex>(
                // Top
                Vertex.builder()
                        .setPosition(Vector3(feature.x, feature.y + TRIANGLE_SIZE, feature.z))
                        .setUvCoordinate(uv0)
                        .setNormal(normalTop)
                        .build(),
                // Left
                Vertex.builder()
                        .setPosition(Vector3(feature.x - TRIANGLE_SIZE, feature.y, feature.z - TRIANGLE_SIZE))
                        .setUvCoordinate(uv0)
                        .setNormal(normalLeft)
                        .build(),
                // Front
                Vertex.builder()
                        .setPosition(Vector3(feature.x, feature.y, feature.z + TRIANGLE_SIZE))
                        .setUvCoordinate(uv0)
                        .setNormal(normalFront)
                        .build(),
                // Right
                Vertex.builder()
                        .setPosition(Vector3(feature.x + TRIANGLE_SIZE, feature.y, feature.z - TRIANGLE_SIZE))
                        .setUvCoordinate(uv0)
                        .setNormal(normalRight)
                        .build()
        )
    }
}