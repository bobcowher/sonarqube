/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.ce.task.projectanalysis.component;

import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.db.component.BranchType;

final class CommunityBranch implements Branch {
  private final String name;
  private final boolean main;
  private final String referenceBranchUuid;

  CommunityBranch(String name, boolean main, @Nullable String referenceBranchUuid) {
    this.name = name;
    this.main = main;
    this.referenceBranchUuid = referenceBranchUuid;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public BranchType getType() {
    return BranchType.BRANCH;
  }

  @Override
  public boolean isMain() {
    return main;
  }

  @Override
  public String getReferenceBranchUuid() {
    return referenceBranchUuid;
  }

  @Override
  public boolean supportsCrossProjectCpd() {
    return main;
  }

  @Override
  public String getPullRequestKey() {
    throw new IllegalStateException("A long-lived branch does not have a pull request key");
  }

  @Override
  public String getTargetBranchName() {
    return null;
  }
}
