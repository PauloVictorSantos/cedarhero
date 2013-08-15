package br.com.ingenieux.cedarhero.mojo.heroku;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;

import java.io.File;
import java.io.IOException;

/**
 * <p>Creates / prepares a Staging Repository for Heroku Deployment</p>
 *
 * <p>A Staging Repository is a git repository (by default, under [basedir]/tmp-git-deployment-staging) made from the following elements:</p>
 *
 * <ul>
 *   <li>A clone of the [sourceStackRepository] git repository (defaults to the jetty runner stack, <a href="https://bitbucket.org/ingenieux/cedarhero-jetty">https://bitbucket.org/ingenieux/cedarhero-jetty</a>)</li>
 *   <li>The webapp contents under /webapp</li>
 * </ul>
 *
 * <p>The sourceStack includes a "stub pom", meant to satisfy herokus' need for a pom.xml.</p>
 *
 * <p>In the future, it would be used to optimize the slug generation process (like assigning/replacing xml config
 * files for addon values, or something like that).</p>
 *
 * <p>For now, a barebones sourceStack git repository includes:</p>
 * <ul>
 *   <li>The basic Procfile</li>
 *   <li>The webapp Runtime</li>
 *   <li>Other settings</li>
 * </ul>
 *
 * <p>You could consider either forking the sourceStackRepository or changing it locally prior to deployment. Future versions will pay more attention to it</p>
 *
 */
@Mojo(name = "prepare")
public class PrepareMojo extends AbstractHerokuMojo {
  /**
   * Artifact to Deploy. Will be copied over to 'webapp'
   */
  @Parameter(property = "heroku.sourceArtifactDirectory", defaultValue = "${project.build.directory}/${project.build.finalName}")
  File sourceArtifactDirectory;

  /**
   * Git Staging Dir (should not be under target/)
   */
  @Parameter(property = "heroku.stagingDirectory", defaultValue = "${project.basedir}/tmp-git-deployment-staging")
  File stagingDirectory;

  /**
   * Git Source Stack Repository (Git URL)
   */
  @Parameter(property = "heroku.sourceStackRepository", defaultValue = "https://bitbucket.org/ingenieux/cedarhero-jetty.git")
  String sourceStackRepository;

  @Override
  protected void executeInternal() throws Exception {
    Git gitRepo = getGitRepo(stagingDirectory);

    File webappStagingDirectory = new File(stagingDirectory, "webapp");

    if (!webappStagingDirectory.exists()) {
      webappStagingDirectory.mkdirs();
    }

    log("Syncing from %s to %s", sourceArtifactDirectory, webappStagingDirectory);

    syncDirs(gitRepo, sourceArtifactDirectory, webappStagingDirectory);

    appendChanges(gitRepo);
  }

  private void syncDirs(Git gitRepo, File sourceArtifactDirectory, File webappStagingDirectory) throws IOException {
    /**
     * First Step: Copy sourceArtifactDirectory to webappStaging (only modified files)
     */
    for (File origFile : FileUtils.listFiles(sourceArtifactDirectory, FileFilterUtils.trueFileFilter(), TrueFileFilter.INSTANCE)) {
      String relPath = origFile.getAbsolutePath().substring(sourceArtifactDirectory.getAbsolutePath().length());

      File otherFile = new File(webappStagingDirectory, relPath);

      boolean mustCopy = false;

      if (otherFile.exists()) {
        /**
         * TBD: Compare by using hashes instead
         */
        mustCopy = (otherFile.lastModified() != origFile.lastModified());
      } else {
        mustCopy = true;
      }

      if (mustCopy) {
        otherFile.getParentFile().mkdirs();

        log("Copying %s to %s", relPath, otherFile.getPath());

        FileUtils.copyFile(origFile, otherFile, true);
      }
    }

    /**
     * Second Step: Remove anything under webappStagingDirectory not in sourceArtifactDirectory
     */
    for (File stagedFile : FileUtils.listFiles(webappStagingDirectory, FileFilterUtils.trueFileFilter(), TrueFileFilter.INSTANCE)) {
      String relPath = stagedFile.getAbsolutePath().substring(webappStagingDirectory.getAbsolutePath().length());

      File sourceFile = new File(sourceArtifactDirectory, relPath);

      boolean mustDelete = false;

      mustDelete = !sourceFile.exists();

      if (mustDelete) {
        log("Excluding: %s", relPath);

        stagedFile.delete();
      }
    }
  }

  private void appendChanges(Git gitRepo) throws GitAPIException, IOException {
    gitRepo.add().setUpdate(true).addFilepattern(".").call();

    // Now as for any new files (untracked)

    String commitId = null;

    Ref masterRef = gitRepo.getRepository()
            .getRef("master");
    if (null != masterRef)
      commitId = ObjectId.toString(masterRef.getObjectId());

    Status status = gitRepo.status().call();

    AddCommand addCommand = gitRepo.add();

    if (!status.getUntracked().isEmpty()) {
      for (String s : status.getUntracked()) {
        log("Adding file %s", s);
        addCommand.addFilepattern(s);
      }

      addCommand.call();
    }

    log("Committing");

    try {
      gitRepo.commit().setMessage("Update from heroku:prepare").setAmend(true).call();
    } catch (Exception exc) {
      getLog().info("Error", exc);
    }
  }

  protected Git getGitRepo(File stagingDirectory) throws Exception {
    Git git = null;

    if (!stagingDirectory.exists()) {
      log("Need to create git repository in %s", stagingDirectory);

      log("Cloning from %s", sourceStackRepository);

      Git.cloneRepository().setNoCheckout(false).setDirectory(stagingDirectory).setURI(sourceStackRepository).setProgressMonitor(new TextProgressMonitor()).call();

      git = Git.open(stagingDirectory);

      StoredConfig config = git.getRepository().getConfig();
      config.unsetSection("remote", "origin");
      try {
        config.save();
      } catch (IOException e) {
        log(e, "Error while removing remote");
      }
    } else {
      git = Git.open(stagingDirectory);
    }

    return git;
  }
}
