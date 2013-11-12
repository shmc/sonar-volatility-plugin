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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.blame.BlameLine;
import org.apache.maven.scm.command.blame.BlameScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.test.IsResource;
import org.sonar.test.TestUtils;

/**
 * @author Evgeny Mandrikov
 */
public class BlameTest {
  private static final String FILENAME = "Foo.txt";

  private ScmManager scmManager;
  private SensorContext context;
  private Blame blame;

  @Before
  public void before() {
    this.scmManager = mock(ScmManager.class);
    this.context = mock(SensorContext.class);
    this.blame = new Blame(this.scmManager, mock(SonarScmRepository.class));
  }

  /**
   * See SONARPLUGINS-368 - can occur with generated sources
   */
  @Test
  public void shouldNotThrowScmException() throws Exception {
    doThrow(new ScmException("ERROR")).when(this.scmManager).blame(any(ScmRepository.class), any(ScmFileSet.class), anyString());

    File file = TestUtils.getResource(getClass(), FILENAME);
    this.blame.retrieveBlame(file);

    // no exception
  }

  @Test
  public void testAnalyse() throws Exception {
    when(this.scmManager.blame(any(ScmRepository.class), any(ScmFileSet.class), anyString()))
        .thenReturn(new BlameScmResult("fake", Arrays.asList(
            new BlameLine(new Date(13), "2", "godin"),
            new BlameLine(new Date(10), "1", "godin"))));

    File file = TestUtils.getResource(getClass(), FILENAME);
    ProjectStatus.FileStatus fileStatus = new ProjectStatus.FileStatus(file, FILENAME);
    this.blame.analyse(fileStatus, new org.sonar.api.resources.File(FILENAME), this.context);

    verify(this.context).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.FILE, FILENAME)),
        argThat(new IsMeasure(CoreMetrics.SCM_LAST_COMMIT_DATE)));
    verify(this.context).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.FILE, FILENAME)),
        argThat(new IsMeasure(CoreMetrics.SCM_AUTHORS_BY_LINE)));
    verify(this.context).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.FILE, FILENAME)),
        argThat(new IsMeasure(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE)));
    verify(this.context).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.FILE, FILENAME)),
        argThat(new IsMeasure(CoreMetrics.SCM_REVISIONS_BY_LINE)));
    verifyNoMoreInteractions(this.context);
  }

}
