package com.ryanhodgman.ar

import com.google.ar.core.PointCloud

val PointCloud.numFeatures: Int
    // Points in the cloud consist of 4 values: x, y, z, and a confidence value
    get() = points.limit() / 4

val PointCloud.features: List<Feature>
    get() = List(numFeatures) { featureIndex ->
        // Points in the cloud consist of 4 values: x, y, z, and a confidence value
        val bufferIndex = featureIndex * 4
        Feature(x = points[bufferIndex],
                y = points[bufferIndex + 1],
                z = points[bufferIndex + 2],
                confidence = points[bufferIndex + 3])
    }

data class Feature(val x: Float, val y: Float, val z: Float, val confidence: Float)