package com.ryanhodgman.ar.nodes

import android.content.Context
import com.google.ar.core.PointCloud
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.rendering.RenderableDefinition.Submesh
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

    private companion object {
        private const val TRIANGLE_SIZE = 0.005f
    }

    private var timestamp: Long = 0
    private var pointBuffer: Array<Vertex?> = arrayOfNulls(0)
    private var indexBuffer: Array<Int?> = arrayOfNulls(0)
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
        val buf = cloud.points
        // Points in the cloud consist of 4 values: x, y, z, and a confidence value
        val numFeatures = buf.limit() / 4
        if (numFeatures < 1) {
            // No features in the cloud to be rendered
            renderable = null
            return
        }
        // Each feature point is drawn as a triangular pyramid
        val vertexPerFeature = 4
        val numFaces = 4
        val numPoints = numFeatures * vertexPerFeature
        val vertexPerFace = 3
        // Draw a triangle per face (4 triangles) per feature
        val indexPerFeature = numFaces * vertexPerFace
        val numIndices = numFeatures * indexPerFeature
        // Allocate a buffer on the high water mark for the given point cloud
        if (pointBuffer.size < numPoints) {
            pointBuffer = arrayOfNulls(numPoints)
            indexBuffer = arrayOfNulls(numIndices)
        }
        // Prepare rendering buffers
        val featurePoint = Vector3()
        val vertices = arrayOf(Vector3(), Vector3(), Vector3(), Vector3())
        val normals = arrayOf(Vector3(0f, 0f, 1f), Vector3(.7f, 0f, .7f), Vector3(-.7f, 0f, .7f), Vector3(0f, 1f, 0f))
        val uv0 = Vertex.UvCoordinate(0f, 0f)
        // Point cloud data is 4 floats per feature, {x, y, z, confidence}
        for (i in 0 until buf.limit() / 4) {
            // Feature point
            featurePoint.x = buf.get(i * 4)
            featurePoint.y = buf.get(i * 4 + 1)
            featurePoint.z = buf.get(i * 4 + 2)
            // Top vertex
            vertices[0].x = featurePoint.x
            vertices[0].y = featurePoint.y + TRIANGLE_SIZE
            vertices[0].z = featurePoint.z
            // Left vertex
            vertices[1].x = featurePoint.x - TRIANGLE_SIZE
            vertices[1].y = featurePoint.y
            vertices[1].z = featurePoint.z - TRIANGLE_SIZE
            // Front vertex
            vertices[2].x = featurePoint.x
            vertices[2].y = featurePoint.y
            vertices[2].z = featurePoint.z + TRIANGLE_SIZE
            // Right vertex
            vertices[3].x = featurePoint.x + TRIANGLE_SIZE
            vertices[3].y = featurePoint.y
            vertices[3].z = featurePoint.z - TRIANGLE_SIZE
            // Create the vertices.  Set the tangent and UV to quiet warnings about material requirements
            val vertexBase = i * vertexPerFeature
            pointBuffer[vertexBase] = Vertex.builder().setPosition(vertices[0])
                .setUvCoordinate(uv0)
                .setNormal(normals[0])
                .build()
            pointBuffer[vertexBase + 1] = Vertex.builder().setPosition(vertices[1])
                .setUvCoordinate(uv0)
                .setNormal(normals[1])
                .build()
            pointBuffer[vertexBase + 2] = Vertex.builder().setPosition(vertices[2])
                .setUvCoordinate(uv0)
                .setNormal(normals[2])
                .build()
            pointBuffer[vertexBase + 3] = Vertex.builder().setPosition(vertices[3])
                .setUvCoordinate(uv0)
                .setNormal(normals[3])
                .build()
            // The indices of the triangles need to be listed counter clockwise as
            // appears when facing the front side of the face.
            val featureBase = i * indexPerFeature
            // left 0 1 2
            indexBuffer[featureBase + 2] = vertexBase
            indexBuffer[featureBase] = vertexBase + 1
            indexBuffer[featureBase + 1] = vertexBase + 2
            // right 0 2 3
            indexBuffer[featureBase + 3] = vertexBase
            indexBuffer[featureBase + 4] = vertexBase + 2
            indexBuffer[featureBase + 5] = vertexBase + 3
            // back 0 3 1
            indexBuffer[featureBase + 6] = vertexBase
            indexBuffer[featureBase + 7] = vertexBase + 3
            indexBuffer[featureBase + 8] = vertexBase + 1
            // bottom 1,2,3
            indexBuffer[featureBase + 9] = vertexBase + 1
            indexBuffer[featureBase + 10] = vertexBase + 2
            indexBuffer[featureBase + 11] = vertexBase + 3
        }
        // Create renderable for the given point cloud
        val submesh = Submesh.builder()
            .setName("pointcloud")
            .setMaterial(material)
            .setTriangleIndices(indexBuffer.toList().subList(0, numIndices))
            .build()
        val renderableDef = RenderableDefinition.builder()
            .setVertices(pointBuffer.toList().subList(0, numPoints))
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
}