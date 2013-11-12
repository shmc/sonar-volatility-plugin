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
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.blame.BlameLine;
import org.apache.maven.scm.command.blame.BlameScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.PropertiesBuilder;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.SonarException;

/**
 * @author Evgeny Mandrikov
 */
public class Blame implements BatchExtension {
	private ScmManager scmManager;
	private SonarScmRepository repositoryBuilder;

	private PropertiesBuilder<Integer, String> authorsBuilder = new PropertiesBuilder<Integer, String>(
			CoreMetrics.SCM_AUTHORS_BY_LINE);
	private PropertiesBuilder<Integer, String> datesBuilder = new PropertiesBuilder<Integer, String>(
			CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE);
	private PropertiesBuilder<Integer, String> revisionsBuilder = new PropertiesBuilder<Integer, String>(
			CoreMetrics.SCM_REVISIONS_BY_LINE);
	private PropertiesBuilder<Integer, Float> volatilityBuilder = new PropertiesBuilder<Integer, Float>(
			CodeVolatilityMetrics.VOLATILITY_PER_LINE);

	public Blame(final ScmManager scmManager, final SonarScmRepository repositoryBuilder) {
		this.scmManager = scmManager;
		this.repositoryBuilder = repositoryBuilder;
	}

	public void analyse(final ProjectStatus.FileStatus fileStatus, final Resource resource, final SensorContext context) {

		BlameScmResult result = retrieveBlame(fileStatus.getFile());

		this.authorsBuilder.clear();
		this.revisionsBuilder.clear();
		this.datesBuilder.clear();
		this.volatilityBuilder.clear();

		double totalVolatility = 0;

		List lines = result.getLines();
		for (int i = 0; i < lines.size(); i++) {
			BlameLine line = (BlameLine) lines.get(i);
			Date date = line.getDate();
			String revision = line.getRevision();
			String author = line.getAuthor();

			int lineNumber = i + 1;
			this.datesBuilder.add(lineNumber, DateUtils.formatDateTime(date));
			this.revisionsBuilder.add(lineNumber, revision);
			this.authorsBuilder.add(lineNumber, author);

			float volatilityIndex = calculateVolatilityIndex(date);
			totalVolatility += volatilityIndex;
			this.volatilityBuilder.add(lineNumber, volatilityIndex);

		}

		saveDataMeasure(context, resource, CoreMetrics.SCM_REVISION, fileStatus.getRevision(), PersistenceMode.FULL);
		saveDataMeasure(context, resource, CoreMetrics.SCM_LAST_COMMIT_DATE,
				ScmUtils.formatLastCommitDate(fileStatus.getDate()), PersistenceMode.FULL);
		saveDataMeasure(context, resource, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, this.datesBuilder.buildData(),
				PersistenceMode.DATABASE);
		saveDataMeasure(context, resource, CoreMetrics.SCM_REVISIONS_BY_LINE, this.revisionsBuilder.buildData(),
				PersistenceMode.DATABASE);
		saveDataMeasure(context, resource, CoreMetrics.SCM_AUTHORS_BY_LINE, this.authorsBuilder.buildData(),
				PersistenceMode.DATABASE);

		double volatilityPerFile = lines.size() > 0 ? totalVolatility / lines.size() : 0;
		
		Logs.INFO.info("Volatility per file " + resource.getName() + " = " + Double.toString(volatilityPerFile));
		
		saveValueMeasure(context, resource, CodeVolatilityMetrics.VOLATILITY_PER_FILE, volatilityPerFile,
				PersistenceMode.FULL);
		saveDataMeasure(context, resource, CodeVolatilityMetrics.VOLATILITY_PER_LINE, this.volatilityBuilder.buildData(),
				PersistenceMode.DATABASE);
	}

	private float calculateVolatilityIndex(final Date lastCommitDate) {
		int lastCommitWeek = new DateTime(lastCommitDate).getWeekOfWeekyear();
		int currentWeek = new DateTime().getWeekOfWeekyear();

		int weekDifference = currentWeek - lastCommitWeek;
		return weekDifference > 0 ? 1f / weekDifference : 1;
	}

	private void saveDataMeasure(final SensorContext context, final Resource resource, final Metric metricKey, final String data,
			final PersistenceMode persistence) {
		if (StringUtils.isNotBlank(data)) {
			context.saveMeasure(resource, new Measure(metricKey, data).setPersistenceMode(persistence));
		}
	}

	private void saveValueMeasure(final SensorContext context, final Resource resource, final Metric metricKey, final Double value,
			final PersistenceMode persistence) {
		context.saveMeasure(resource, new Measure(metricKey, value).setPersistenceMode(persistence));
	}

	BlameScmResult retrieveBlame(final File file) {
		try {
			Logs.INFO.info("Retrieve SCM info for " + file);
			BlameScmResult result = this.scmManager.blame(this.repositoryBuilder.getScmRepository(),
					new ScmFileSet(file.getParentFile()), file.getName());
			if (!result.isSuccess()) {
				throw new SonarException("Fail to retrieve SCM info for file " + file + ": "
						+ result.getProviderMessage() + SystemUtils.LINE_SEPARATOR + result.getCommandOutput());
			}
			return result;

		} catch (ScmException e) {
			// See SONARPLUGINS-368. Can occur on generated source
			LoggerFactory.getLogger(getClass()).debug("Unable to retrieve SCM info of: " + file, e);
			return null;
		}
	}
}
