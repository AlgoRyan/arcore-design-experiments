package com.ryanhodgman.ar

import com.google.ar.core.Plane

/**
 * Calculates the area of the plane based on its polygonal extents.
 *
 * @return An extent measurement in m^2
 */
fun Plane.calcPolygonalArea(): Double {
    val polygonExtents = polygon.array()
    val vertices = polygonExtents.foldIndexed(mutableListOf<Vertex>()) { index, accum, _ ->
        if (index % 2 == 0) {
            // Only apply to every second element
            accum.add(Vertex(polygonExtents[index], polygonExtents[index + 1]))
        }
        accum
    }
    return calcShoelaceArea(vertices)
}

/**
 * Source - https://rosettacode.org/wiki/Shoelace_formula_for_polygonal_area#Kotlin
 */
private fun calcShoelaceArea(vertices: List<Vertex>): Double {
    val numVertices = vertices.size
    var area = 0.0
    for (i in 0 until numVertices - 1) {
        area += vertices[i].x * vertices[i + 1].y - vertices[i + 1].x * vertices[i].y
    }
    return Math.abs(area + vertices[numVertices - 1].x * vertices[0].y - vertices[0].x * vertices[numVertices - 1].y) / 2.0
}

private data class Vertex(val x: Float, val y: Float)