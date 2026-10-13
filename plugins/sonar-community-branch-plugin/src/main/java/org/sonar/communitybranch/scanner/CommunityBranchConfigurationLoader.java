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

import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchConfigurationLoader;
import org.sonar.scanner.scan.branch.DefaultBranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;

public class CommunityBranchConfigurationLoader implements BranchConfigurationLoader {
  static final String BRANCH_NAME = "sonar.branch.name";
  private static final String PULL_REQUEST_PREFIX = "sonar.pullrequest.";

  private final BranchConfigurationFactory branchConfigurationFactory;

  public CommunityBranchConfigurationLoader(BranchConfigurationFactory branchConfigurationFactory) {
    this.branchConfigurationFactory = branchConfigurationFactory;
  }

  @Override
  public BranchConfiguration load(Map<String, String> localSettings, ProjectBranches projectBranches) {
    boolean hasPullRequestParameter = localSettings.entrySet().stream()
      .anyMatch(entry -> entry.getKey().startsWith(PULL_REQUEST_PREFIX) && StringUtils.isNotBlank(entry.getValue()));
    if (hasPullRequestParameter) {
      throw MessageException.of("Pull request analysis is not supported by this Community Build fork");
    }

    String branchName = Optional.ofNullable(StringUtils.trimToNull(localSettings.get(BRANCH_NAME))).orElse(null);
    if (branchName == null) {
      return new DefaultBranchConfiguration();
    }
    return branchConfigurationFactory.create(branchName, projectBranches);
  }
}
