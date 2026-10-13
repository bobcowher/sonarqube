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

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.http.service.ServiceClient;
import org.sonar.scanner.http.transport.HttpResponse;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonar.scanner.scan.branch.ProjectBranchesLoader;

public class CommunityProjectBranchesLoader implements ProjectBranchesLoader {
  private static final String LIST_URL = "/api/project_branches/list?project=";

  private final ServiceClient serviceClient;
  private final Gson gson = GsonHelper.create();

  public CommunityProjectBranchesLoader(ServiceClient serviceClient) {
    this.serviceClient = serviceClient;
  }

  @Override
  public ProjectBranches load(String projectKey) {
    String path = LIST_URL + URLEncoder.encode(projectKey, StandardCharsets.UTF_8);
    try (HttpResponse response = serviceClient.get(path)) {
      if (response.code() == 404) {
        return new ProjectBranches(new ArrayList<>());
      }
      if (!response.isSuccessful()) {
        throw MessageException.of("Could not load branches from server: HTTP " + response.code());
      }
      try (Reader reader = response.bodyReader()) {
        BranchesResponse branchesResponse = gson.fromJson(reader, BranchesResponse.class);
        return new ProjectBranches(branchesResponse.branches);
      }
    } catch (IOException e) {
      throw MessageException.of("Could not load branches from server", e);
    }
  }

  private static final class BranchesResponse {
    private List<BranchInfo> branches;
  }
}
