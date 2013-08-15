package br.com.ingenieux.cedarhero.mojo.heroku;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.CredentialsProvider;

import java.io.File;

import static org.apache.commons.lang.StringUtils.isBlank;

@Mojo(name = "deploy")
public class DeployMojo extends AbstractHerokuMojo {
  /**
   * Git Staging Dir (should not be under target/)
   */
  @Parameter(property = "heroku.stagingDirectory", defaultValue = "${project.basedir}/tmp-git-deployment-staging")
  File stagingDirectory;

  /**
   * The server id in maven settings.xml to use for AWS Services Credentials
   * (accessKey / secretKey)
   */
  @Parameter(property = "heroku.sshKey", defaultValue = "${user.home}/.ssh/id_rsa")
  protected String sshKey;

  /**
   * The App Name to push
   */
  @Parameter(property = "heroku.app")
  protected String app;

  @Override
  protected void executeInternal() throws Exception {
    if (isBlank(app))
      throw new MojoFailureException("app is not set");

    if (!stagingDirectory.exists())
      throw new MojoFailureException("Inexistant staging directory. Create one with heroku:prepare");

    Git repo = Git.open(stagingDirectory);

    String remoteUrl = "ssh://git@heroku.com:" + app + ".git";

    log("Pushing from %s to %s", stagingDirectory.getParent(), remoteUrl);

    PushCommand pushCommand = repo.push();

    CredentialsProvider cp ;
    pushCommand.setCredentialsProvider(cp);

    pushCommand.setRemote(remoteUrl).add("master").setProgressMonitor(new TextProgressMonitor()).setForce(true).call();

    //repo.push().setRemote("")
  }
}
