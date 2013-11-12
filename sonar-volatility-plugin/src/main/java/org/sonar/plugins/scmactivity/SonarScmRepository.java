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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.sonar.api.BatchExtension;
import org.sonar.api.utils.SonarException;

public class SonarScmRepository implements BatchExtension {
  private ScmManager manager;
  private ScmConfiguration conf;
  private ScmRepository repository;

  public SonarScmRepository(final ScmManager manager, final ScmConfiguration conf) {
    this.manager = manager;
    this.conf = conf;
  }

  public ScmRepository getScmRepository() {
    try {
      if (this.repository == null) {
        String connectionUrl = this.conf.getUrl();
        this.repository = this.manager.makeScmRepository(connectionUrl);
        String user = this.conf.getUser();
        String password = this.conf.getPassword();
        if (!StringUtils.isBlank(user) && !StringUtils.isBlank(password)) {
          ScmProviderRepository providerRepository = this.repository.getProviderRepository();
          providerRepository.setUser(user);
          providerRepository.setPassword(password);
        }
      }
      return this.repository;

    } catch (ScmException e) {
      throw new SonarException(e);
    }
  }

  public void clean() {
    this.repository = null;
  }
}
