package br.com.ingenieux.cedarhero.mojo.heroku;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.CredentialsProviderUserInfo;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;

import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

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

		String remoteUrl = "git@heroku.com:" + app + ".git";
		
		StoredConfig config = repo.getRepository().getConfig();
		
		config.setString("remote", "heroku", "url", remoteUrl);
		
		config.save();

		log("Pushing from %s to %s", stagingDirectory, remoteUrl);

		JschConfigSessionFactory sessionFactory = new JschConfigSessionFactory() {
			@Override
			protected void configure(OpenSshConfig.Host hc, Session session) {
				session.setConfig("StrictHostKeyChecking", "no");
				session.setConfig("IdentityFile", sshKey);
				
				CredentialsProvider provider = new CredentialsProvider() {
					@Override
					public boolean isInteractive() {
						return false;
					}

					@Override
					public boolean supports(CredentialItem... items) {
						return true;
					}

					@Override
					public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
						//for (CredentialItem item : items) {
						//	((CredentialItem.StringType) item).setValue("yourpassphrase");
						//}
						return true;
					}
				};
				UserInfo userInfo = new CredentialsProviderUserInfo(session, provider);
				session.setUserInfo(userInfo);
			}
		};
		SshSessionFactory.setInstance(sessionFactory);

		PushCommand pushCommand = repo.push().setRemote(remoteUrl).add("master").setProgressMonitor(new TextProgressMonitor()).setForce(true);
				
		pushCommand.call();

		// repo.push().setRemote("")
	}
}
