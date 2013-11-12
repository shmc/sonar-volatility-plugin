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

import org.apache.maven.scm.ChangeSet;

/**
 * @author Evgeny Mandrikov
 */
public abstract class Changeable {

  private Date date = new Date(0);
  private String revision;
  private String author;
  private int changes;

  public final void add(final ChangeSet changeSet) {
    this.changes++;
    Date changeSetDate = changeSet.getDate();
    if (changeSetDate.after(this.date)) {
      this.date = changeSetDate;
      this.revision = changeSet.getRevision();
      this.author = changeSet.getAuthor();
    }
    doAdd(changeSet);
  }

  protected void doAdd(final ChangeSet changeSet) {
    
  }

  /**
   * @return date of last change
   */
  public Date getDate() {
    return (Date) this.date.clone();
  }

  /**
   * @return revision of last change
   */
  public String getRevision() {
    return this.revision;
  }

  /**
   * @return author of last change
   */
  public String getAuthor() {
    return this.author;
  }

  /**
   * @return number of changes
   */
  public int getChanges() {
    return this.changes;
  }

  public boolean isModified() {
    return this.changes > 0;
  }

}
