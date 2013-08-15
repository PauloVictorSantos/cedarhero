package br.com.ingenieux.cedarhero.mojo.heroku;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name = "reset")
public class ResetMojo extends AbstractHerokuMojo {
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

  @Override
  protected void executeInternal() throws Exception {
  }
}
