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
package org.sonar.communitybranch;

import java.net.URLClassLoader;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.rule.Severity;

/**
 * Scanner plugins normally see only the public plugin API. Branch configuration is an internal scanner extension,
 * so this bootstrap loads the scanner-side implementation with the scanner core classloader as its parent.
 */
public class CommunityBranchPluginBootstrap implements Plugin {
  @Override
  public void define(Context context) {
    if (context.getRuntime().getSonarQubeSide() != SonarQubeSide.SCANNER) {
      return;
    }
    try {
      ClassLoader classLoader = createElevatedClassLoader();
      Class<?> implementation = classLoader.loadClass(CommunityBranchPluginBootstrap.class.getName().replace("Bootstrap", ""));
      Plugin plugin = (Plugin) implementation.getDeclaredConstructor().newInstance();
      plugin.define(context);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Could not load Community Build branch scanner extensions", e);
    }
  }

  private ClassLoader createElevatedClassLoader() {
    ClassLoader pluginClassLoader = getClass().getClassLoader();
    if (!(pluginClassLoader instanceof URLClassLoader urlClassLoader)) {
      throw new IllegalStateException("Expected plugin classloader to be a URLClassLoader but got " + pluginClassLoader.getClass().getName());
    }
    ClassLoader pluginWithCoreParent = URLClassLoader.newInstance(urlClassLoader.getURLs(), Severity.class.getClassLoader());
    return new ClassLoader() {
      @Override
      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
          return pluginWithCoreParent.loadClass(name);
        } catch (ClassNotFoundException e) {
          return pluginClassLoader.loadClass(name);
        }
      }
    };
  }
}
