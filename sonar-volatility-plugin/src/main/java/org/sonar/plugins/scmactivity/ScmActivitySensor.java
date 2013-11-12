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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.plugins.scmactivity.ProjectStatus.FileStatus;

/**
 * @author Evgeny Mandrikov
 */
public final class ScmActivitySensor implements Sensor {
private static final Logger LOG = LoggerFactory.getLogger(ScmActivitySensor.class);

  private TimeMachine timeMachine;
  private ScmConfiguration conf;
  private LocalModificationChecker checkLocalModifications;
  private Changelog changelog;
  private Blame blameSensor;

  public ScmActivitySensor(final ScmConfiguration conf, final LocalModificationChecker checkLocalModifications, final Changelog changelog, final Blame blameSensor, final TimeMachine timeMachine) {
    this.conf = conf;
    this.checkLocalModifications = checkLocalModifications;
    this.changelog = changelog;
    this.blameSensor = blameSensor;
    this.timeMachine = timeMachine;
  }

  @DependedUpon
  public final List<Metric> generatesMetrics() {
    return Arrays.asList(
        CoreMetrics.SCM_REVISION,
        CoreMetrics.SCM_LAST_COMMIT_DATE,
        CoreMetrics.SCM_COMMITS,
        CoreMetrics.SCM_AUTHORS_BY_LINE,
        CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE,
        CoreMetrics.SCM_REVISIONS_BY_LINE);
  }

  public boolean shouldExecuteOnProject(final Project project) {
    return this.conf.isEnabled();
  }

  public void analyse(final Project project, final SensorContext context) {
    this.checkLocalModifications.check();

    ProjectStatus projectStatus = new ProjectStatus(project);
    this.changelog.load(projectStatus, getPreviousRevision(project));

    TimeProfiler profiler = new TimeProfiler().start("Retrieve files SCM info");
    LOG.debug(String.format("%d files touched in changelog", projectStatus.getFileStatuses().size()));
    for (FileStatus fileStatus : projectStatus.getFileStatuses()) {
      inspectFile(context, fileStatus);
    }
    profiler.stop();
    inspectProject(context, project, projectStatus);
  }

  private void inspectFile(final SensorContext context, final FileStatus fileStatus) {
    Resource resource = toResource(context, fileStatus);
    if (resource == null) {
      LOG.debug("File not found in Sonar index: " + fileStatus.getFile());
      return;
    }

    if (fileStatus.isModified()) {
      this.blameSensor.analyse(fileStatus, resource, context);

    } else {
      LOG.debug("File not changed since previous analysis: " + fileStatus.getFile());
      copyPreviousFileMeasures(resource, context);
    }
  }

  private void copyPreviousFileMeasures(final Resource resource, final SensorContext context) {
    List<Measure> previousMeasures = getPreviousMeasures(resource, CoreMetrics.SCM_REVISION, CoreMetrics.SCM_LAST_COMMIT_DATE,
        CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, CoreMetrics.SCM_REVISIONS_BY_LINE, CoreMetrics.SCM_AUTHORS_BY_LINE, CodeVolatilityMetrics.VOLATILITY_PER_FILE, CodeVolatilityMetrics.VOLATILITY_PER_LINE);

    for (Measure previousMeasure : previousMeasures) {
    	
      PersistenceMode persistence = previousMeasure.getMetric().isDataType() ? PersistenceMode.DATABASE : PersistenceMode.FULL;
      
      if (StringUtils.isNotBlank( previousMeasure.getData())) {
        context.saveMeasure(resource, new Measure(previousMeasure.getMetric(),  previousMeasure.getData())).setPersistenceMode(persistence);
      } else if (previousMeasure.getValue() != null) {
    	context.saveMeasure(resource, new Measure(previousMeasure.getMetric(),  previousMeasure.getValue())).setPersistenceMode(persistence);
      }
    }
  }

  private Resource toResource(final SensorContext context, final FileStatus fileStatus) {
    Resource resource = null;
    if (this.conf.isJavaProject()) {
      if (Java.isJavaFile(fileStatus.getFile())) {
        resource = JavaFile.fromRelativePath(fileStatus.getRelativePath(), false);
      }
    } else {
      resource = new org.sonar.api.resources.File(fileStatus.getRelativePath());
    }
    if (resource != null) {
      return context.getResource(resource);
    }
    return null;
  }

  private void inspectProject(final SensorContext context, final Project project, final ProjectStatus projectStatus) {
    final String revision;
    final String date;
    if (projectStatus.isModified()) {
      revision = projectStatus.getRevision();
      date = ScmUtils.formatLastCommitDate(projectStatus.getDate());
    } else {
      revision = getPreviousMeasure(project, CoreMetrics.SCM_REVISION);
      date = getPreviousMeasure(project, CoreMetrics.SCM_LAST_COMMIT_DATE);
    }
    Double commits = getPreviousMeasureValue(project, CoreMetrics.SCM_COMMITS);
    if (commits == null) {
      commits = 0d;
    }
    commits += projectStatus.getChanges();

    context.saveMeasure(new Measure(CoreMetrics.SCM_REVISION, revision));
    context.saveMeasure(new Measure(CoreMetrics.SCM_LAST_COMMIT_DATE, date));
    context.saveMeasure(new Measure(CoreMetrics.SCM_COMMITS, commits));
  }

  private Double getPreviousMeasureValue(final Resource resource, final Metric metric) {
    TimeMachineQuery query = new TimeMachineQuery(resource)
        .setOnlyLastAnalysis(true)
        .setMetrics(metric);
    List<Object[]> fields = this.timeMachine.getMeasuresFields(query);
    if (fields.isEmpty()) {
      return null;
    }
    return (Double) fields.get(0)[2];
  }

  private String getPreviousMeasure(final Resource resource, final Metric metric) {
    List<Measure> measures = getPreviousMeasures(resource, metric);
    if (measures.isEmpty()) {
      return null;
    }
    return measures.get(0).getData();
  }

  private List<Measure> getPreviousMeasures(final Resource resource, final Metric... metrics) {
    TimeMachineQuery query = new TimeMachineQuery(resource)
        .setOnlyLastAnalysis(true)
        .setMetrics(metrics);
    return this.timeMachine.getMeasures(query);
  }


  private String getPreviousRevision(final Project project) {
    // warning: upgrade from SCM plugin 1.1 to 1.2 must be detected.
    // Data stored with SCM 1.1 is not enough for this analysis so it must be ignored.
    // The existence of new metrics of 1.2 (last_commit_date for example) is checked
    List<Measure> measures = getPreviousMeasures(project, CoreMetrics.SCM_REVISION, CoreMetrics.SCM_LAST_COMMIT_DATE);
    if (measures.size()==2) {
      for (Measure measure : measures) {
        if (measure.getMetric().equals(CoreMetrics.SCM_REVISION)) {
          return measure.getData();
        }
      }
    }
    return null;
  }


  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
