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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.DefaultBranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommunityBranchConfigurationLoaderTest {
  private final CommunityBranchConfigurationLoader underTest =
    new CommunityBranchConfigurationLoader(new BranchConfigurationFactory());

  @Test
  void loadsDefaultConfigurationWhenBranchNameIsAbsent() {
    BranchConfiguration result = underTest.load(Map.of(), emptyProjectBranches());

    assertThat(result).isInstanceOf(DefaultBranchConfiguration.class);
  }

  @Test
  void loadsLongLivedBranchConfiguration() {
    BranchConfiguration result = underTest.load(Map.of("sonar.branch.name", " feature/one "), emptyProjectBranches());

    assertThat(result.branchType()).isEqualTo(BranchType.BRANCH);
    assertThat(result.branchName()).isEqualTo("feature/one");
    assertThat(result.referenceBranchName()).isNull();
  }

  @Test
  void rejectsPullRequestConfiguration() {
    Map<String, String> properties = Map.of("sonar.pullrequest.key", "42");

    assertThatThrownBy(() -> underTest.load(properties, emptyProjectBranches()))
      .isInstanceOf(MessageException.class)
      .hasMessage("Pull request analysis is not supported by this Community Build fork");
  }

  private static ProjectBranches emptyProjectBranches() {
    return new ProjectBranches(List.of());
  }
}
