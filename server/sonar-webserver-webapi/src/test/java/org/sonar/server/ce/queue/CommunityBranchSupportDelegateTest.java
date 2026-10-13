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
package org.sonar.server.ce.queue;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.core.ce.CeTaskCharacteristics;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.component.BranchType;
import org.sonar.server.setting.ProjectConfigurationLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CommunityBranchSupportDelegateTest {
  private final CommunityBranchSupportDelegate underTest = new CommunityBranchSupportDelegate(
    mock(UuidFactory.class), mock(DbClient.class), mock(Clock.class), mock(ProjectConfigurationLoader.class));

  @Test
  void createsLongLivedBranchComponentKey() {
    BranchSupport.ComponentKey key = underTest.createComponentKey("project", Map.of(
      CeTaskCharacteristics.BRANCH_TYPE, BranchType.BRANCH.name(),
      CeTaskCharacteristics.BRANCH, " feature/one "));

    assertThat(key.getKey()).isEqualTo("project");
    assertThat(key.getBranchName()).contains("feature/one");
    assertThat(key.getPullRequestKey()).isEqualTo(Optional.empty());
  }

  @Test
  void createsMainBranchComponentKeyWhenCharacteristicsAreEmpty() {
    BranchSupport.ComponentKey key = underTest.createComponentKey("project", Map.of());

    assertThat(key.isMainBranch()).isTrue();
  }

  @Test
  void rejectsPullRequestAnalysis() {
    Map<String, String> characteristics = Map.of(
      CeTaskCharacteristics.BRANCH_TYPE, BranchType.PULL_REQUEST.name(),
      CeTaskCharacteristics.PULL_REQUEST, "42");

    assertThatThrownBy(() -> underTest.createComponentKey("project", characteristics))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Only long-lived branch analysis is supported; unsupported branch type 'PULL_REQUEST'");
  }
}
