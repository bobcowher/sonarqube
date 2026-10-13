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
package org.sonar.communitybranch.scanner;

import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchType;

final class CommunityBranchConfiguration implements BranchConfiguration {
  private final String branchName;
  private final String referenceBranchName;

  CommunityBranchConfiguration(String branchName, String referenceBranchName) {
    this.branchName = branchName;
    this.referenceBranchName = referenceBranchName;
  }

  @Override
  public BranchType branchType() {
    return BranchType.BRANCH;
  }

  @Override
  public String branchName() {
    return branchName;
  }

  @Override
  public String referenceBranchName() {
    return referenceBranchName;
  }

  @Override
  public String targetBranchName() {
    return null;
  }

  @Override
  public String pullRequestKey() {
    throw new IllegalStateException("A long-lived branch does not have a pull request key");
  }
}
