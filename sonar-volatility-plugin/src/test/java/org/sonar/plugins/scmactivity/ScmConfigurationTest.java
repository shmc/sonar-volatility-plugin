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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Project;

public class ScmConfigurationTest {

  private MavenScmConfiguration mavenConf;
  private PropertiesConfiguration configuration;
  private ScmConfiguration scmConfiguration;

  @Before
  public void setUp() {
    this.mavenConf = mock(MavenScmConfiguration.class);
    this.configuration = new PropertiesConfiguration();
    this.scmConfiguration = new ScmConfiguration(new Project("key"), this.configuration, this.mavenConf);
  }

  @Test
  public void shouldReturnUsername() {
    this.configuration.addProperty(ScmActivityPlugin.USER_PROPERTY, "godin");
    assertThat(this.scmConfiguration.getUser(), is("godin"));
  }

  @Test
  public void shouldReturnPassword() {
    this.configuration.addProperty(ScmActivityPlugin.PASSWORD_PROPERTY, "pass");
    assertThat(this.scmConfiguration.getPassword(), is("pass"));
  }

  @Test
  public void shouldReturnUrlFromConfiguration() {
    this.configuration.addProperty(ScmActivityPlugin.URL_PROPERTY, "http://test");
    assertThat(this.scmConfiguration.getUrl(), is("http://test"));
  }

  @Test
  public void shouldBeDisabledIfNoUrl() {
    this.configuration.addProperty(ScmActivityPlugin.ENABLED_PROPERTY, true);
    assertThat(this.scmConfiguration.isEnabled(), is(false));
  }

  @Test
  public void shouldBeDisabledByDefault() {
    assertThat(this.scmConfiguration.isEnabled(), is(false));
  }


  @Test
  public void shouldBeEnabled() {
    this.configuration.addProperty(ScmActivityPlugin.ENABLED_PROPERTY, true);
    this.configuration.addProperty(ScmActivityPlugin.URL_PROPERTY, "scm:http:xxx");
    assertThat(this.scmConfiguration.isEnabled(), is(true));
  }


  @Test
  public void shouldGetMavenDeveloperUrlIfCredentials() {
    when(this.mavenConf.getDeveloperUrl()).thenReturn("scm:https:writable");
    this.configuration.addProperty(ScmActivityPlugin.USER_PROPERTY, "godin");
    this.configuration.addProperty(ScmActivityPlugin.PASSWORD_PROPERTY, "pass");

    assertThat(this.scmConfiguration.getUrl(), is("scm:https:writable"));
  }

  @Test
  public void shouldNotGetMavenDeveloperUrlIfNoCredentials() {
    when(this.mavenConf.getDeveloperUrl()).thenReturn("scm:https:writable");
    when(this.mavenConf.getUrl()).thenReturn("scm:https:readonly");
    
    assertThat(this.scmConfiguration.getUrl(), is("scm:https:readonly"));
  }

  @Test
  public void shouldGetMavenUrlIfNoDeveloperUrl() {
    when(this.mavenConf.getUrl()).thenReturn("scm:http:readonly");
    assertThat(this.scmConfiguration.getUrl(), is("scm:http:readonly"));
  }

  @Test
  public void shouldOverrideMavenUrl() {
    when(this.mavenConf.getUrl()).thenReturn("scm:http:readonly");
    this.configuration.addProperty(ScmActivityPlugin.URL_PROPERTY, "scm:http:override");

    assertThat(this.scmConfiguration.getUrl(), is("scm:http:override"));
  }
}
