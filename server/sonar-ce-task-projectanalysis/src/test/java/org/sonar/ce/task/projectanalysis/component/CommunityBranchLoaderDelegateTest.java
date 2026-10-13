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

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.project.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityBranchLoaderDelegateTest {
  private final DbClient dbClient = mock(DbClient.class);
  private final MutableAnalysisMetadataHolder metadataHolder = mock(MutableAnalysisMetadataHolder.class);
  private final CommunityBranchLoaderDelegate underTest = new CommunityBranchLoaderDelegate(dbClient, metadataHolder);

  @Test
  void loadsBranchAndReferenceBranch() {
    DbSession dbSession = mock(DbSession.class);
    BranchDao branchDao = mock(BranchDao.class);
    BranchDto branch = new BranchDto().setKey("feature").setUuid("feature-uuid").setBranchType(BranchType.BRANCH).setIsMain(false);
    BranchDto main = new BranchDto().setKey("main").setUuid("main-uuid").setBranchType(BranchType.BRANCH).setIsMain(true);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(branchDao.selectByBranchKey(dbSession, "project-uuid", "feature")).thenReturn(Optional.of(branch));
    when(branchDao.selectByBranchKey(dbSession, "project-uuid", "main")).thenReturn(Optional.of(main));
    when(metadataHolder.getProject()).thenReturn(new Project("project-uuid", "key", "name", null, List.of()));
    ScannerReport.Metadata metadata = ScannerReport.Metadata.newBuilder()
      .setBranchType(ScannerReport.Metadata.BranchType.BRANCH)
      .setBranchName("feature")
      .setReferenceBranchName("main")
      .build();

    underTest.load(metadata);

    ArgumentCaptor<Branch> branchCaptor = ArgumentCaptor.captor();
    verify(metadataHolder).setBranch(branchCaptor.capture());
    assertThat(branchCaptor.getValue().getName()).isEqualTo("feature");
    assertThat(branchCaptor.getValue().getReferenceBranchUuid()).isEqualTo("main-uuid");
    assertThat(branchCaptor.getValue().isMain()).isFalse();
  }

  @Test
  void rejectsPullRequestMetadata() {
    when(metadataHolder.getProject()).thenReturn(new Project("project-uuid", "key", "name", null, List.of()));
    ScannerReport.Metadata metadata = ScannerReport.Metadata.newBuilder()
      .setBranchType(ScannerReport.Metadata.BranchType.PULL_REQUEST)
      .build();

    assertThatThrownBy(() -> underTest.load(metadata))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Pull request analysis is not supported by the Community Build branch extension");
  }
}
