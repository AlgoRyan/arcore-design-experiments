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
 * [Original implementation][https://github.com/claywilkinson/arcore-android-sdk/blob/sceneform-samples/samples/cloud_anchor_java/app/src/main/java/com/google/ar/core/examples/java/cloudanchor/sceneform/PointCloudNode.java]
 * by [Clayton Wilkinson][https://github.com/claywilkinson]
 *
 * @author Clayton Wilkinson, with modifications by Ryan Hodgman
 */
class PointCloudNode : Node() {

    private var timestamp: Long = 0
    private var material: Material? = null

    suspend fun prepare(context: Context) {
        material = suspendCoroutine { continuation ->
            val color = Color().apply {
                set(android.graphics.Color.RED)
            }
            MaterialFactory.makeOpaqueWithColor(context, color)
                    .thenAccept { continuation.resume(it) }
        }
    }

    /**
     * Update the renderable for the point cloud. This creates a small quad for each feature point.
     */
    fun update(cloud: PointCloud) {
        // Skip if disabled, if the cloud has not been updated, or if the material hasn't yet been loaded
        if (!isEnabled || timestamp == cloud.timestamp || material == null) return
        timestamp = cloud.timestamp
        if (cloud.numFeatures < 1) {
            // No features in the cloud to be rendered
            renderable = null
            return
        }
        // Begin collection of the vertices and face indices that represent the structure of the desired Renderable
        val indices = mutableListOf<Int>()
        val vertices = mutableListOf<Vertex>()
        cloud.features.forEachIndexed { featureIndex, feature ->
            PyramidMesh(feature).also { mesh ->
                indices.addAll(mesh.indices.map { meshIndex ->
                    // Each index matches to a vertex, and there are four vertices per feature
                    featureIndex * 4 + meshIndex
                })
                vertices.addAll(mesh.vertices)
            }
        }
        // Create Renderable for the given point cloud
        val submesh = Submesh.builder()
                .setName("pointcloud")
                .setMaterial(material)
                .setTriangleIndices(indices)
                .build()
        val renderableDef = RenderableDefinition.builder()
                .setVertices(vertices)
                .setSubmeshes(listOf(submesh))
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