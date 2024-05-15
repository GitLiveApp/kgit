/*******************************************************************************
 * Copyright (c) 2014 Konrad Kügler and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.merge

import org.eclipse.jgit.api.MergeCommand.FastForwardMode
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Config.SectionParser
import org.eclipse.jgit.lib.ConfigConstants
import org.eclipse.jgit.lib.Repository
import java.io.IOException

/**
 * Holds configuration for merging into a given branch
 *
 * @since 3.3
 */
class MergeConfig {
    /**
     * Get the fast forward mode configured for this branch
     *
     * @return the fast forward mode configured for this branch
     */
	val fastForwardMode: FastForwardMode

    /**
     * Whether merges into this branch are configured to be squash merges, false
     * otherwise
     *
     * @return true if merges into this branch are configured to be squash
     * merges, false otherwise
     */
    val isSquash: Boolean

    /**
     * Whether `--no-commit` option is not set.
     *
     * @return `false` if --no-commit is configured for this branch,
     * `true` otherwise (even if --squash is configured)
     */
    val isCommit: Boolean

    private constructor(branch: String, config: Config) {
        val mergeOptions = getMergeOptions(branch, config)
        fastForwardMode = getFastForwardMode(config, mergeOptions)
        isSquash = isMergeConfigOptionSet("--squash", mergeOptions) //$NON-NLS-1$
        isCommit = !isMergeConfigOptionSet("--no-commit", mergeOptions) //$NON-NLS-1$
    }

    private constructor() {
        fastForwardMode = FastForwardMode.FF
        isSquash = false
        isCommit = true
    }

    private class MergeConfigSectionParser(private val branch: String) : SectionParser<MergeConfig> {
        override fun parse(cfg: Config): MergeConfig {
            return MergeConfig(branch, cfg)
        }

        override fun equals(obj: Any?): Boolean {
            if (obj is MergeConfigSectionParser) {
                return branch == obj.branch
            }
            return false
        }

        override fun hashCode(): Int {
            return branch.hashCode()
        }
    }

    companion object {
        /**
         * Get merge configuration for the current branch of the repository
         *
         * @param repo
         * a [org.eclipse.jgit.lib.Repository] object.
         * @return merge configuration for the current branch of the repository
         */
		@JvmStatic
		fun getConfigForCurrentBranch(repo: Repository): MergeConfig {
            try {
                val branch = repo.branch
                if (branch != null) return repo.config.get(getParser(branch))
            } catch (e: IOException) {
                // ignore
            }
            // use defaults if branch can't be determined
            return MergeConfig()
        }

        /**
         * Get a parser for use with
         * [org.eclipse.jgit.lib.Config.get]
         *
         * @param branch
         * short branch name to get the configuration for, as returned
         * e.g. by [org.eclipse.jgit.lib.Repository.getBranch]
         * @return a parser for use with
         * [org.eclipse.jgit.lib.Config.get]
         */
		@JvmStatic
		fun getParser(
            branch: String
        ): SectionParser<MergeConfig> {
            return MergeConfigSectionParser(branch)
        }

        private fun getFastForwardMode(
            config: Config,
            mergeOptions: Array<String?>
        ): FastForwardMode {
            for (option in mergeOptions) {
                for (mode in FastForwardMode.entries) if (mode.matchConfigValue(option)) return mode
            }
            val ffmode = FastForwardMode.valueOf(
                config.getEnum(
                    ConfigConstants.CONFIG_KEY_MERGE, null,
                    ConfigConstants.CONFIG_KEY_FF, FastForwardMode.Merge.TRUE
                )
            )
            return ffmode
        }

        private fun isMergeConfigOptionSet(
            optionToLookFor: String,
            mergeOptions: Array<String?>
        ): Boolean {
            for (option in mergeOptions) {
                if (optionToLookFor == option) return true
            }
            return false
        }

        private fun getMergeOptions(branch: String, config: Config): Array<String?> {
            val mergeOptions = config.getString(
                ConfigConstants.CONFIG_BRANCH_SECTION, branch,
                ConfigConstants.CONFIG_KEY_MERGEOPTIONS
            )
            if (mergeOptions != null) {
                return mergeOptions.split("\\s".toRegex()) //$NON-NLS-1$
                    .dropLastWhile { it.isEmpty() }.toTypedArray()
            }
            return arrayOfNulls(0)
        }
    }
}
