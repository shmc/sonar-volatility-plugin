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

import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

public final class CodeVolatilityMetrics implements Metrics {

	public static final String DOMAIN = "Volatility";

	public static final Metric VOLATILITY_PER_LINE = new Metric.Builder("volatility_index_per_line",
			"Volatility /line", Metric.ValueType.DATA)
			.setDescription("1/ age in weeks (from Monday to Sunday)").setDirection(Metric.DIRECTION_WORST)
			.setQualitative(false).setDomain(DOMAIN).create();

	public static final Metric VOLATILITY_PER_FILE = new Metric.Builder("volatility_index_per_file",
			"Volatility /file", Metric.ValueType.FLOAT)
			.setDescription("avg(Volatility index per line)").setDirection(Metric.DIRECTION_WORST)
			.setQualitative(false).setBestValue(0d).setWorstValue(1d).setDomain(DOMAIN).create();

	public List<Metric> getMetrics() {
		return Arrays.asList(VOLATILITY_PER_LINE, VOLATILITY_PER_FILE);
	}
}
