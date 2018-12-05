package com.ryanhodgman.ar.nodes

import com.google.ar.sceneform.Node
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * Specifies a pattern for providing scopes to coroutines run within [Node] objects.
 *
 * @author Ryan Hodgman
 */
open class CoroutineNode : Node(), CoroutineScope {

    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    //region Lifecycle
    override fun onActivate() {
        super.onActivate()
        job = Job()
    }

    override fun onDeactivate() {
        job.cancel()
        super.onDeactivate()
    }
    //endregion
}