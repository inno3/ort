/*
 * Copyright (C) 2019 HERE Europe B.V.
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

package org.ossreviewtoolkit.helper.commands.repoconfig

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.types.file

import org.ossreviewtoolkit.helper.common.sortEntries
import org.ossreviewtoolkit.helper.common.write
import org.ossreviewtoolkit.model.config.RepositoryConfiguration
import org.ossreviewtoolkit.model.readValue
import org.ossreviewtoolkit.utils.expandTilde

internal class SortCommand : CliktCommand(
    help = "Sorts all exclude and curation entries of the given repository configuration alphabetically. The output " +
            "is written to the given repository configuration file."
) {
    private val repositoryConfigurationFile by argument(
        "repository-configuration-file",
        help = "The repository configuration file to be sorted."
    ).convert { it.expandTilde() }
        .file(mustExist = true, canBeFile = true, canBeDir = false, mustBeWritable = false, mustBeReadable = false)
        .convert { it.absoluteFile.normalize() }

    override fun run() {
        repositoryConfigurationFile
            .readValue<RepositoryConfiguration>()
            .sortEntries()
            .write(repositoryConfigurationFile)
    }
}
