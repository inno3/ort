/*
 * Copyright (C) 2017-2019 HERE Europe B.V.
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

package org.ossreviewtoolkit.downloader.vcs

import io.kotest.core.spec.Spec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.containExactlyInAnyOrder
import io.kotest.matchers.maps.beEmpty
import io.kotest.matchers.should

import java.io.File

import org.ossreviewtoolkit.downloader.VersionControlSystem
import org.ossreviewtoolkit.model.VcsInfo
import org.ossreviewtoolkit.model.VcsType
import org.ossreviewtoolkit.utils.ORT_NAME
import org.ossreviewtoolkit.utils.ortDataDirectory
import org.ossreviewtoolkit.utils.safeDeleteRecursively
import org.ossreviewtoolkit.utils.unpack

class GitWorkingTreeTest : StringSpec() {
    private val git = Git()
    private lateinit var zipContentDir: File

    override fun beforeSpec(spec: Spec) {
        val zipFile = File("src/funTest/assets/pipdeptree-2018-01-03-git.zip")

        zipContentDir = createTempDir(ORT_NAME, javaClass.simpleName)

        println("Extracting '$zipFile' to '$zipContentDir'...")
        zipFile.unpack(zipContentDir)
    }

    override fun afterSpec(spec: Spec) {
        zipContentDir.safeDeleteRecursively(force = true)
    }

    init {
        "Detected Git version is not empty" {
            val version = git.getVersion()
            println("Git version $version detected.")
            version shouldNotBe ""
        }

        "Git detects non-working-trees" {
            git.getWorkingTree(ortDataDirectory).isValid() shouldBe false
        }

        "Git correctly detects URLs to remote repositories" {
            // Bitbucket forwards to ".git" URLs for Git repositories, so we can omit the suffix.
            git.isApplicableUrl("https://bitbucket.org/yevster/spdxtraxample") shouldBe true

            git.isApplicableUrl("https://bitbucket.org/paniq/masagin") shouldBe false
        }

        "Git does not prompt for credentials for non-existing repositories" {
            git.isApplicableUrl("https://github.com/oss-review-toolkit/foobar.git") shouldBe false
        }

        "Detected Git working tree information is correct" {
            val workingTree = git.getWorkingTree(zipContentDir)

            workingTree.isValid() shouldBe true
            workingTree.getInfo() shouldBe VcsInfo(
                type = VcsType.GIT,
                url = "https://github.com/naiquevin/pipdeptree.git",
                revision = "6f70dd5508331b6cfcfe3c1b626d57d9836cfd7c",
                resolvedRevision = null,
                path = ""
            )
            workingTree.getNested() should beEmpty()
            workingTree.getRootPath() shouldBe zipContentDir
            workingTree.getPathToRoot(File(zipContentDir, "tests")) shouldBe "tests"
        }

        "Git correctly lists remote branches" {
            val workingTree = git.getWorkingTree(zipContentDir)

            workingTree.listRemoteBranches() should containExactlyInAnyOrder(
                "debug-test-failures",
                "drop-py2.6",
                "fixing-test-setups",
                "master",
                "release-0.10.1",
                "reverse-mode",
                "v2beta"
            )
        }

        "Git correctly lists remote tags" {
            val workingTree = git.getWorkingTree(zipContentDir)

            workingTree.listRemoteTags() should containExactlyInAnyOrder(
                "0.10.0",
                "0.10.1",
                "0.11.0",
                "0.12.0",
                "0.12.1",
                "0.13.0",
                "0.13.1",
                "0.13.2",
                "0.5.0",
                "0.6.0",
                "0.7.0",
                "0.8.0",
                "0.9.0",
                "1.0.0"
            )
        }

        "Git correctly lists submodules" {
            val expectedSubmodules = listOf(
                "analyzer/src/funTest/assets/projects/external/dart-http",
                "analyzer/src/funTest/assets/projects/external/directories",
                "analyzer/src/funTest/assets/projects/external/example-python-flask",
                "analyzer/src/funTest/assets/projects/external/jgnash",
                "analyzer/src/funTest/assets/projects/external/quickcheck-state-machine",
                "analyzer/src/funTest/assets/projects/external/sbt-multi-project-example",
                "analyzer/src/funTest/assets/projects/external/spdx-tools-python"
            ).associateWith { VersionControlSystem.getPathInfo(File("../$it")) }

            val workingTree = git.getWorkingTree(File(".."))
            workingTree.getNested() shouldBe expectedSubmodules
        }
    }
}
