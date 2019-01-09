package com.ryanhodgman.ar.data

import android.content.Context
import java.io.File
import java.io.FileWriter

/**
 * Responsible for managing data collected around AR experiment sessions.
 *
 * @author Ryan Hodgman
 */
class ExperimentDataRepository(context: Context) {

    private val appDir = context.getExternalFilesDir(null)
    private val experimentDataDir = File(appDir, "Experiment Data").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Creates a new experiment record on the file system.
     */
    fun storeExperimentData(data: ExperimentData) {
        val newExperiment = File(experimentDataDir, "Experiment ${experimentDataDir.listFiles().size + 1}.txt")
        FileWriter(newExperiment).apply {
            append(data.toString())
            flush()
            close()
        }
    }

    class ExperimentData {

        private val records = mutableListOf<Record>()

        fun addRecord(record: Record) {
            records.add(record)
        }

        override fun toString(): String {
            val stringBuilder = StringBuilder(records.size * 20)
            records.forEach {
                stringBuilder.append(it.toString())
                stringBuilder.append("\n")
            }
            return stringBuilder.toString()
        }

        data class Record(val timeMs: Long, val numFeaturesTracked: Int, val avgFeatureConfidence: Float,
                          val planeSizeMetres: Double) {

            override fun toString(): String = "%.2f | %03d %.2f %.2f"
                    .format(timeMs / 1000.0, numFeaturesTracked, avgFeatureConfidence, planeSizeMetres)
        }
    }
}