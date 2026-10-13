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
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.config.Configuration;
import org.sonar.core.ce.CeTaskCharacteristics;
import org.sonar.core.config.PurgeConstants;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.setting.ProjectConfigurationLoader;

/** Community Build implementation of report submission for long-lived branches. */
public class CommunityBranchSupportDelegate implements BranchSupportDelegate {
  private final UuidFactory uuidFactory;
  private final DbClient dbClient;
  private final Clock clock;
  private final ProjectConfigurationLoader projectConfigurationLoader;

  public CommunityBranchSupportDelegate(UuidFactory uuidFactory, DbClient dbClient, Clock clock,
    ProjectConfigurationLoader projectConfigurationLoader) {
    this.uuidFactory = uuidFactory;
    this.dbClient = dbClient;
    this.clock = clock;
    this.projectConfigurationLoader = projectConfigurationLoader;
  }

  @Override
  public BranchSupport.ComponentKey createComponentKey(String projectKey, Map<String, String> characteristics) {
    String branchType = StringUtils.trimToNull(characteristics.get(CeTaskCharacteristics.BRANCH_TYPE));
    String branchName = StringUtils.trimToNull(characteristics.get(CeTaskCharacteristics.BRANCH));

    if (branchType == null && branchName == null) {
      return new CommunityComponentKey(projectKey, null);
    }
    if (!BranchType.BRANCH.name().equals(branchType)) {
      throw new IllegalArgumentException("Only long-lived branch analysis is supported; unsupported branch type '" + branchType + "'");
    }
    if (branchName == null) {
      throw new IllegalArgumentException("Missing branch characteristic '" + CeTaskCharacteristics.BRANCH + "'");
    }
    return new CommunityComponentKey(projectKey, branchName);
  }

  @Override
  public ComponentDto createBranchComponent(DbSession dbSession, BranchSupport.ComponentKey componentKey,
    ComponentDto mainComponentDto, BranchDto mainComponentBranchDto) {
    if (!componentKey.getKey().equals(mainComponentDto.getKey())) {
      throw new IllegalStateException("Component key and main component key do not match");
    }

    String branchName = componentKey.getBranchName()
      .orElseThrow(() -> new IllegalArgumentException("A branch name is required"));
    String branchUuid = uuidFactory.create();
    ComponentDto componentDto = mainComponentDto.copy()
      .setUuid(branchUuid)
      .setBranchUuid(branchUuid)
      .setUuidPath(ComponentDto.UUID_PATH_OF_ROOT)
      .setCreatedAt(new Date(clock.millis()));
    dbClient.componentDao().insert(dbSession, componentDto, false);

    BranchDto branchDto = new BranchDto()
      .setProjectUuid(mainComponentBranchDto.getProjectUuid())
      .setUuid(branchUuid)
      .setBranchType(BranchType.BRANCH)
      .setExcludeFromPurge(isBranchExcludedFromPurge(
        projectConfigurationLoader.loadProjectConfiguration(dbSession, mainComponentBranchDto.getProjectUuid()), branchName))
      .setKey(branchName)
      .setIsMain(false);
    dbClient.branchDao().insert(dbSession, branchDto);
    return componentDto;
  }

  private static boolean isBranchExcludedFromPurge(Configuration projectConfiguration, String branchName) {
    return Arrays.stream(projectConfiguration.getStringArray(PurgeConstants.BRANCHES_TO_KEEP_WHEN_INACTIVE))
      .map(Pattern::compile)
      .map(Pattern::asMatchPredicate)
      .anyMatch(predicate -> predicate.test(branchName));
  }

  private static final class CommunityComponentKey extends BranchSupport.ComponentKey {
    private final String projectKey;
    private final String branchName;

    private CommunityComponentKey(String projectKey, String branchName) {
      this.projectKey = projectKey;
      this.branchName = branchName;
    }

    @Override
    public String getKey() {
      return projectKey;
    }

    @Override
    public Optional<String> getBranchName() {
      return Optional.ofNullable(branchName);
    }

    @Override
    public Optional<String> getPullRequestKey() {
      return Optional.empty();
    }
  }
}
