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

import org.apache.maven.scm.log.ScmLogger;
import org.apache.maven.scm.manager.AbstractScmManager;
import org.apache.maven.scm.provider.accurev.AccuRevScmProvider;
import org.apache.maven.scm.provider.bazaar.BazaarScmProvider;
import org.apache.maven.scm.provider.clearcase.ClearCaseScmProvider;
import org.apache.maven.scm.provider.cvslib.cvsexe.CvsExeScmProvider;
import org.apache.maven.scm.provider.git.gitexe.SonarGitExeScmProvider;
import org.apache.maven.scm.provider.hg.HgScmProvider;
import org.apache.maven.scm.provider.perforce.PerforceScmProvider;
import org.apache.maven.scm.provider.svn.svnexe.SonarSvnExeScmProvider;
import org.apache.maven.scm.provider.tfs.TfsScmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;

/**
 * @author Evgeny Mandrikov
 */
public class SonarScmManager extends AbstractScmManager implements BatchExtension {

  public SonarScmManager(final ScmConfiguration conf) {
    if (conf.isEnabled()) {
      setScmProvider("svn", new SonarSvnExeScmProvider());
      setScmProvider("cvs", new CvsExeScmProvider());
      setScmProvider("git", new SonarGitExeScmProvider());
      setScmProvider("hg", new HgScmProvider());
      setScmProvider("bazaar", new BazaarScmProvider());
      setScmProvider("clearcase", new ClearCaseScmProvider());
      setScmProvider("accurev", new AccuRevScmProvider());
      setScmProvider("perforce", new PerforceScmProvider());
      setScmProvider("tfs", new TfsScmProvider());
    }
  }

  @Override
  protected ScmLogger getScmLogger() {
    return new SonarScmLogger(LoggerFactory.getLogger(getClass()));
  }

  private static class SonarScmLogger implements ScmLogger {
    private Logger log;

    SonarScmLogger(final Logger log) {
      this.log = log;
    }

    public boolean isDebugEnabled() {
      return this.log.isDebugEnabled();
    }

    public void debug(final String content) {
      this.log.debug(content);
    }

    public void debug(final String content, final Throwable error) {
      this.log.debug(content, error);
    }

    public void debug(final Throwable error) {
      this.log.debug(error.getMessage(), error);
    }

    public boolean isInfoEnabled() {
      return this.log.isInfoEnabled();
    }

    public void info(final String content) {
      this.log.info("\t" + content);
    }

    public void info(final String content, final Throwable error) {
      this.log.info("\t" + content, error);
    }

    public void info(final Throwable error) {
      this.log.info("\t" + error.getMessage(), error);
    }

    public boolean isWarnEnabled() {
      return this.log.isWarnEnabled();
    }

    public void warn(final String content) {
      this.log.warn(content);
    }

    public void warn(final String content, final Throwable error) {
      this.log.warn(content, error);
    }

    public void warn(final Throwable error) {
      this.log.warn(error.getMessage(), error);
    }

    public boolean isErrorEnabled() {
      return this.log.isErrorEnabled();
    }

    public void error(final String content) {
      this.log.error(content);
    }

    public void error(final String content, final Throwable error) {
      this.log.error(content, error);
    }

    public void error(final Throwable error) {
      this.log.error(error.getMessage(), error);
    }
  }
}
