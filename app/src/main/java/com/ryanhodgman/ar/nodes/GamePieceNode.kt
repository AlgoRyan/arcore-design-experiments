package com.ryanhodgman.ar.nodes

import android.content.Context
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.BaseTransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import com.ryanhodgman.ar.GridTranslationController

class GamePieceNode(transformationSystem: TransformationSystem) : BaseTransformableNode(transformationSystem) {

    fun prepare(context: Context) {
        val blue = Color().apply {
            set(android.graphics.Color.BLUE)
        }
        MaterialFactory.makeOpaqueWithColor(context, blue)
            .thenAccept { material ->
                renderable = ShapeFactory.makeCube(
                    Vector3(0.1f, 0.1f, 0.1f),
                    Vector3(0f, 0f, 0f),
                    material
                )
            }
        addTransformationController(GridTranslationController(this, transformationSystem.dragRecognizer))
    }
}