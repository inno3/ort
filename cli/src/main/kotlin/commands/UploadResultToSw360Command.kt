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

package org.ossreviewtoolkit.cli.commands

import com.fasterxml.jackson.databind.DeserializationFeature

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file

import org.eclipse.sw360.clients.adapter.SW360ProjectClientAdapter
import org.eclipse.sw360.clients.adapter.SW360ReleaseClientAdapter
import org.eclipse.sw360.clients.rest.resource.SW360Visibility
import org.eclipse.sw360.clients.rest.resource.projects.SW360Project
import org.eclipse.sw360.clients.rest.resource.releases.SW360Release
import org.eclipse.sw360.clients.utils.SW360ClientException

import org.ossreviewtoolkit.cli.GlobalOptions
import org.ossreviewtoolkit.cli.readOrtResult
import org.ossreviewtoolkit.cli.utils.inputGroup
import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.OrtResult
import org.ossreviewtoolkit.model.Package
import org.ossreviewtoolkit.model.Project
import org.ossreviewtoolkit.model.config.Sw360StorageConfiguration
import org.ossreviewtoolkit.model.jsonMapper
import org.ossreviewtoolkit.model.utils.toPurl
import org.ossreviewtoolkit.scanner.storages.Sw360Storage
import org.ossreviewtoolkit.utils.collectMessagesAsString
import org.ossreviewtoolkit.utils.expandTilde
import org.ossreviewtoolkit.utils.log

class UploadResultToSw360Command : CliktCommand(
    name = "upload-result-to-sw360",
    help = "Upload an ORT result to SW360.",
    epilog = "EXPERIMENTAL: The command is still in development and usage will likely change in the near future. The " +
            "command expects that a Sw360Storage for the scanner is configured."
) {
    private val ortFile by option(
        "--ort-file", "-i",
        help = "The ORT result file to read as input."
    ).convert { it.expandTilde() }
        .file(mustExist = true, canBeFile = true, canBeDir = false, mustBeWritable = false, mustBeReadable = true)
        .convert { it.absoluteFile.normalize() }
        .required()
        .inputGroup()

    private val globalOptionsForSubcommands by requireObject<GlobalOptions>()

    override fun run() {
        val ortResult = readOrtResult(ortFile)

        val sw360Config = globalOptionsForSubcommands.config.scanner.storages?.values
            ?.filterIsInstance<Sw360StorageConfiguration>()?.singleOrNull()

        requireNotNull(sw360Config) {
            "No SW360 storage is configured for the scanner."
        }

        val sw360JsonMapper = jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val sw360Connection = Sw360Storage.createConnection(sw360Config, sw360JsonMapper)
        val sw360ReleaseClient = sw360Connection.releaseAdapter
        val sw360ProjectClient = sw360Connection.projectAdapter

        getProjectWithPackages(ortResult).forEach { (project, pkgList) ->
            val linkedReleases = pkgList.mapNotNull { pkg ->
                val name = createReleaseName(pkg.id)
                sw360ReleaseClient.getSparseReleaseByNameAndVersion(name, pkg.id.version)
                    .flatMap { sw360ReleaseClient.enrichSparseRelease(it) }
                    .orElseGet { createSw360Release(pkg, sw360ReleaseClient) }
            }

            val sw360Project = sw360ProjectClient.getProjectByNameAndVersion(project.id.name, project.id.version)
                .orElseGet { createSw360Project(project, sw360ProjectClient) }

            sw360Project?.let {
                sw360ProjectClient.addSW360ReleasesToSW360Project(it.id, linkedReleases)
            }
        }
    }

    private fun createSw360Project(project: Project, client: SW360ProjectClientAdapter): SW360Project? {
        val sw360Project = SW360Project()
            .setName(project.id.name)
            .setVersion(project.id.version)
            .setDescription("A ${project.id.type} project with the PURL ${project.id.toPurl()}.")
            .setVisibility(SW360Visibility.BUISNESSUNIT_AND_MODERATORS)

        return try {
            client.createProject(sw360Project)?.also {
                log.debug { "Project '${it.name}-${it.version}' created in SW360." }
            }
        } catch (e: SW360ClientException) {
            log.error {
                "Could not create the project '${project.id.toCoordinates()}' in SW360: " + e.collectMessagesAsString()
            }

            null
        }
    }

    private fun createSw360Release(pkg: Package, client: SW360ReleaseClientAdapter): SW360Release? {
        // TODO: This omits operators and exceptions from licenses. We yet need to find a way to pass these to SW360.
        val licenseShortNames = pkg.declaredLicensesProcessed.spdxExpression?.licenses().orEmpty().toSortedSet()

        val unmappedLicenses = pkg.declaredLicensesProcessed.unmapped.toSortedSet()
        if (unmappedLicenses.isNotEmpty()) {
            log.warn {
                "The following licenses could not be mapped in order to create a SW360 release: $unmappedLicenses"
            }
        }

        val sw360Release = SW360Release()
            .setMainLicenseIds(licenseShortNames)
            .setName(createReleaseName(pkg.id))
            .setVersion(pkg.id.version)

        return try {
            client.createRelease(sw360Release)?.also {
                log.debug { "Release '${it.name}-${it.version}' created in SW360." }
            }
        } catch (e: SW360ClientException) {
            log.error {
                "Could not create the release for '${pkg.id.toCoordinates()}' in SW360: " + e.collectMessagesAsString()
            }

            null
        }
    }

    private fun getProjectWithPackages(ortResult: OrtResult): Map<Project, List<Package>> =
        ortResult.getProjects(omitExcluded = true).associateWith { project ->
            // Upload the uncurated packages because SW360 also is a package curation provider.
            project.collectDependencies().mapNotNull { ortResult.getUncuratedPackageById(it) }
        }

    private fun createReleaseName(pkgId: Identifier) =
        listOf(pkgId.namespace, pkgId.name).filter { it.isNotEmpty() }.joinToString("/")
}
