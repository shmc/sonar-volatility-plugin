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
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;

import com.google.common.collect.Lists;

public class ScmConfiguration implements BatchExtension {

  private Configuration conf;
  private MavenScmConfiguration mavenConf;
  private ProjectFileSystem fileSystem;
  private boolean isJavaProject;
  private boolean isIgnoreLocalModifications;

  public ScmConfiguration(final Project project, final Configuration configuration, final MavenScmConfiguration mavenConfiguration) {
    this.fileSystem = project.getFileSystem();
    this.conf = configuration;
    this.mavenConf = mavenConfiguration;
    this.isJavaProject = Java.KEY.equals(project.getLanguageKey());
    this.isIgnoreLocalModifications = configuration.getBoolean(ScmActivityPlugin.IGNORE_LOCAL_MODIFICATIONS, ScmActivityPlugin.IGNORE_LOCAL_MODIFICATIONS_DEFAULT_VALUE);
  }

  public ScmConfiguration(final Project project, final Configuration configuration) {
    this(project, configuration, null /* not in maven environment */);
  }

  public boolean isEnabled() {
    return this.conf.getBoolean(ScmActivityPlugin.ENABLED_PROPERTY, ScmActivityPlugin.ENABLED_DEFAULT_VALUE) && getUrl() != null;
  }

  public String getUser() {
    return this.conf.getString(ScmActivityPlugin.USER_PROPERTY);
  }

  public String getPassword() {
    return this.conf.getString(ScmActivityPlugin.PASSWORD_PROPERTY);
  }

  public File getBaseDir() {
    return this.fileSystem.getBasedir();
  }

  public boolean isJavaProject() {
    return this.isJavaProject;
  }

  public boolean isIgnoreLocalModifications() {
    return this.isIgnoreLocalModifications;
  }

  public List<File> getSourceDirs() {
    List<File> dirs = Lists.newArrayList();
    dirs.addAll(this.fileSystem.getSourceDirs());
    dirs.addAll(this.fileSystem.getTestDirs());
    return dirs;
  }

  public boolean isVerbose() {
    return this.conf.getBoolean(ScmActivityPlugin.VERBOSE_PROPERTY, ScmActivityPlugin.VERBOSE_DEFAULT_VALUE);
  }

  public File getWorkdir() {
    return this.fileSystem.getSonarWorkingDirectory();
  }

  public String getUrl() {
    String url = this.conf.getString(ScmActivityPlugin.URL_PROPERTY);
    if (StringUtils.isBlank(url)) {
      url = getMavenUrl();
    }
    return StringUtils.defaultIfBlank(url, null);
  }

  private String getMavenUrl() {
    String url = null;
    if (this.mavenConf != null) {
      if (StringUtils.isNotBlank(this.mavenConf.getDeveloperUrl()) && StringUtils.isNotBlank(getUser()) && StringUtils.isNotBlank(getPassword())) {
        url = this.mavenConf.getDeveloperUrl();
      } else {
        url = this.mavenConf.getUrl();
      }
    }
    return url;
  }

}
