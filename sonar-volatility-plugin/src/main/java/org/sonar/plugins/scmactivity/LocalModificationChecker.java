/*
 * Sonar Volatility Plugin
 * Copyright (C) 2011 LeanDo Technologies
 * mis@leandotech.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.scmactivity;

import java.io.File;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;

import com.google.common.base.Joiner;

public class LocalModificationChecker implements BatchExtension {

  private ScmManager manager;
  private SonarScmRepository repository;
  private ScmConfiguration config;

  public LocalModificationChecker(final ScmConfiguration config, final ScmManager manager, final SonarScmRepository repository) {
    this.config = config;
    this.manager = manager;
    this.repository = repository;
  }

  public void check() {
    if (!this.config.isIgnoreLocalModifications()) {
      doCheck();
    }
  }

  void doCheck() {
    TimeProfiler profiler = new TimeProfiler().start("Check for local modifications");
    try {
      for (File sourceDir : this.config.getSourceDirs()) {
        // limitation of http://jira.codehaus.org/browse/SONAR-2266, the directory existence must be checked
        if (sourceDir.exists()) {
          LoggerFactory.getLogger(getClass()).debug("Check directory: " + sourceDir);
          StatusScmResult result = this.manager.status(this.repository.getScmRepository(), new ScmFileSet(sourceDir));

          if (!result.isSuccess()) {
            throw new SonarException("Unable to check for local modifications: " + result.getProviderMessage());
          }

          if (!result.getChangedFiles().isEmpty()) {
            Joiner joiner = Joiner.on(SystemUtils.LINE_SEPARATOR + "\t");
            throw new SonarException("Fail to load SCM data as there are local modifications: " + SystemUtils.LINE_SEPARATOR + "\t" + joiner.join(result.getChangedFiles()));
          }
        }
      }

    } catch (ScmException e) {
      throw new SonarException("Unable to check for local modifications", e);

    } finally {
      profiler.stop();
    }
  }

}
