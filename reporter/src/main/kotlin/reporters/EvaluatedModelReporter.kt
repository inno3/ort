/*
 * Copyright (C) 2017-2020 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.reporter.reporters

import java.io.File

import kotlin.time.measureTimedValue

import org.ossreviewtoolkit.model.FileFormat
import org.ossreviewtoolkit.reporter.Reporter
import org.ossreviewtoolkit.reporter.ReporterInput
import org.ossreviewtoolkit.reporter.model.EvaluatedModel
import org.ossreviewtoolkit.utils.log
import org.ossreviewtoolkit.utils.perf

/**
 * A [Reporter] that generates an [EvaluatedModel].
 *
 * This reporter supports the following options:
 * - *output.file.formats*: The list of [FileFormat]s to generate, defaults to [FileFormat.JSON].
 */
class EvaluatedModelReporter : Reporter {
    companion object {
        const val OPTION_OUTPUT_FILE_FORMATS = "output.file.formats"
    }

    override val reporterName = "EvaluatedModel"

    override fun generateReport(
        input: ReporterInput,
        outputDir: File,
        options: Map<String, String>
    ): List<File> {
        val evaluatedModel = measureTimedValue { EvaluatedModel.create(input) }

        log.perf { "Generating evaluated model took ${evaluatedModel.duration.inWholeMilliseconds}ms." }

        val outputFiles = mutableListOf<File>()
        val outputFileFormats = options[OPTION_OUTPUT_FILE_FORMATS]
            ?.split(',')
            ?.mapTo(mutableSetOf()) { FileFormat.forExtension(it) }
            ?: setOf(FileFormat.JSON)

        outputFileFormats.forEach { fileFormat ->
            val outputFile = outputDir.resolve("evaluated-model.${fileFormat.fileExtension}")

            outputFile.bufferedWriter().use {
                when (fileFormat) {
                    FileFormat.JSON -> evaluatedModel.value.toJson(it)
                    FileFormat.YAML -> evaluatedModel.value.toYaml(it)
                    else -> throw IllegalArgumentException("Unsupported Evaluated Model file format '$fileFormat'.")
                }
            }

            outputFiles += outputFile
        }

        return outputFiles
    }
}
