package br.com.ingenieux.cedarhero.mojo.heroku;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;

import java.io.File;
import java.io.IOException;

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

    if (webappStagingDirectory.exists()) {
      FileUtils.deleteDirectory(webappStagingDirectory);
    }

    webappStagingDirectory.mkdirs();

    log("Copying from %s to %s", sourceArtifactDirectory, webappStagingDirectory);

    FileUtils.copyDirectory(sourceArtifactDirectory, webappStagingDirectory);

    appendChanges(gitRepo);


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
