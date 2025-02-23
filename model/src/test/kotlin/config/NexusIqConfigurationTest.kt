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

package org.ossreviewtoolkit.model.config

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

import java.io.File

import org.ossreviewtoolkit.model.readValue
import org.ossreviewtoolkit.model.writeValue
import org.ossreviewtoolkit.utils.test.createTestTempFile

class NexusIqConfigurationTest : WordSpec({
    "NexusIqConfiguration" should {
        "support a serialization round-trip via an ObjectMapper" {
            val ortConfig = OrtConfiguration.load(file = File("src/main/resources/reference.conf"))
            val rereadOrtConfig = createTestTempFile(suffix = ".yml").run {
                writeValue(ortConfig)
                readValue<OrtConfiguration>()
            }

            val actualNexusIqConfiguration = rereadOrtConfig.advisor.nexusIq
            val expectedNexusIqConfig = ortConfig.advisor.nexusIq

            expectedNexusIqConfig.shouldBeInstanceOf<NexusIqConfiguration>()
            actualNexusIqConfiguration.shouldBeInstanceOf<NexusIqConfiguration>()
            actualNexusIqConfiguration.serverUrl shouldBe expectedNexusIqConfig.serverUrl
            actualNexusIqConfiguration.username shouldBe expectedNexusIqConfig.username
        }
    }
})
