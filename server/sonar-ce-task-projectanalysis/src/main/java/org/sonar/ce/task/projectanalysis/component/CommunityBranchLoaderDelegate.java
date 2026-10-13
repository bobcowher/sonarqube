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

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.scanner.protocol.output.ScannerReport;

/** Loads long-lived branch metadata emitted by the bundled scanner extension. */
public class CommunityBranchLoaderDelegate implements BranchLoaderDelegate {
  private final DbClient dbClient;
  private final MutableAnalysisMetadataHolder metadataHolder;

  public CommunityBranchLoaderDelegate(DbClient dbClient, MutableAnalysisMetadataHolder metadataHolder) {
    this.dbClient = dbClient;
    this.metadataHolder = metadataHolder;
  }

  @Override
  public void load(ScannerReport.Metadata metadata) {
    String projectUuid = metadataHolder.getProject().getUuid();
    String branchName = StringUtils.trimToNull(metadata.getBranchName());
    if (metadata.getBranchType() == ScannerReport.Metadata.BranchType.PULL_REQUEST) {
      throw new IllegalStateException("Pull request analysis is not supported by the Community Build branch extension");
    }

    BranchDto currentBranch = branchName == null
      ? findMainBranch(projectUuid).orElseThrow(() -> new IllegalStateException("Could not find main branch"))
      : findBranch(projectUuid, branchName).orElseThrow(() -> new IllegalStateException("Could not find branch '" + branchName + "'"));

    String referenceBranchName = StringUtils.trimToNull(metadata.getReferenceBranchName());
    String referenceBranchUuid = currentBranch.isMain() ? null : resolveReferenceBranchUuid(projectUuid, currentBranch, referenceBranchName);

    metadataHolder.setBranch(new CommunityBranch(currentBranch.getKey(), currentBranch.isMain(), referenceBranchUuid));
    metadataHolder.setPullRequestKey("");
  }

  private String resolveReferenceBranchUuid(String projectUuid, BranchDto currentBranch, String referenceBranchName) {
    if (referenceBranchName == null) {
      return currentBranch.getUuid();
    }
    return findBranch(projectUuid, referenceBranchName)
      .map(BranchDto::getUuid)
      .orElseThrow(() -> new IllegalStateException("Could not find reference branch '" + referenceBranchName + "'"));
  }

  private Optional<BranchDto> findMainBranch(String projectUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.branchDao().selectMainBranchByProjectUuid(dbSession, projectUuid);
    }
  }

  private Optional<BranchDto> findBranch(String projectUuid, String key) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.branchDao().selectByBranchKey(dbSession, projectUuid, key);
    }
  }
}
