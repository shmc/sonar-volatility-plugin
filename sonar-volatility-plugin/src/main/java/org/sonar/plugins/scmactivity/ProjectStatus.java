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

import org.apache.maven.scm.ChangeSet;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;

import com.google.common.collect.Lists;

/**
 * @author Evgeny Mandrikov
 */
public final class ProjectStatus extends Changeable {

  private List<FileStatus> fileStatuses = Lists.newArrayList();

  public ProjectStatus(final Project project) {
    this(project.getFileSystem());
  }

  public ProjectStatus(final ProjectFileSystem fileSystem) {
    for (InputFile inputFile : fileSystem.mainFiles()) {
      this.fileStatuses.add(new FileStatus(inputFile));
    }
  }

  public ProjectStatus(final List<java.io.File> fileStatuses) {
    for (File file : fileStatuses) {
      this.fileStatuses.add(new FileStatus(file, file.getName()));
    }
  }

  @Override
protected void doAdd(final ChangeSet changeSet) {
    for (FileStatus status : this.fileStatuses) {
      if (changeSet.containsFilename(status.getRelativePath())) {
        status.add(changeSet);
      }
    }
  }

  public List<FileStatus> getFileStatuses() {
    return this.fileStatuses;
  }

  static final class FileStatus extends Changeable {
    private File file;
    private String relativePath;

    FileStatus(final InputFile inputFile) {
      this.file = inputFile.getFile();
      this.relativePath = inputFile.getRelativePath();
    }

    FileStatus(final File file, final String relativePath) {
      this.file = file;
      this.relativePath = relativePath;
    }

    public File getFile() {
      return this.file;
    }

    public String getRelativePath() {
      return this.relativePath;
    }
  }

}
