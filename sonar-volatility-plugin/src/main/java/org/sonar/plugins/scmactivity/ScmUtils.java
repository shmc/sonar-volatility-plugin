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

import java.util.Date;

import org.sonar.api.utils.DateUtils;

/**
 * @author Evgeny Mandrikov
 */
public final class ScmUtils {

  private ScmUtils() {
  }

  public static String formatLastCommitDate(final Date date) {
    return DateUtils.formatDate(date);
  }

  public static Date parseLastCommitDate(final String s) {
    return DateUtils.parseDate(s);
  }
}
