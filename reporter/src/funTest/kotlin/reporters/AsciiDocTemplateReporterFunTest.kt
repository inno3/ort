/*
 * Copyright (C) 2020 Bosch.IO GmbH
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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.TestConfiguration
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.beInRange
import io.kotest.matchers.should

import java.io.File

import org.ossreviewtoolkit.model.OrtResult
import org.ossreviewtoolkit.reporter.ORT_RESULT
import org.ossreviewtoolkit.reporter.ReporterInput
import org.ossreviewtoolkit.utils.test.createTestTempDir

class AsciiDocTemplateReporterFunTest : StringSpec({
    "PDF output is created successfully from an existing result and default template" {
        val report = generateReport(ORT_RESULT)

        report.single().length() should beInRange(92000L..97000L)
    }

    "Report generation is aborted when path to non-existing pdf-them file is given" {
        shouldThrow<IllegalArgumentException> {
            generateReport(ORT_RESULT, mapOf("pdf.theme.file" to "dummy.file"))
        }
    }

    "PDF output is aborted when a non-existent PDF fonts directory is given" {
        shouldThrow<IllegalArgumentException> {
            generateReport(ORT_RESULT, mapOf("pdf.fonts.dir" to "fake.path"))
        }
    }
})

private fun TestConfiguration.generateReport(
    ortResult: OrtResult,
    options: Map<String, String> = emptyMap()
): List<File> {
    val input = ReporterInput(ortResult)
    val outputDir = createTestTempDir()

    return AsciiDocTemplateReporter().generateReport(input, outputDir, options)
}
