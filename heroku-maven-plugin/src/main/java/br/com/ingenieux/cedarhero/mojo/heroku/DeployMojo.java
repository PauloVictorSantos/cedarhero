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

/**
 * <p>Deploys the staging (prepared) repository under Heroku.
 *
 * <p>Obligatory showoff example (for existing java projects):</p>
 *
 * <pre>
 *   &lt;plugins&gt;
 *     &lt;plugin&gt;
 *       &lt;groupId&gt;br.com.ingenieux.cedarhero&lt;/groupId&gt;
 *       &lt;artifactId&gt;heroku-maven-plugin&lt;/artifactId&gt;
 *       &lt;configuration&gt;
 *         &lt;app&gt;lovely-app-7777&lt;/app&gt;
 *       &lt;/configuration&gt;
 *     &lt;/plugin&gt;
 *   &lt;/plugins&gt;
 * </pre>
 *
 * <p>Once pom is configured (and you've got a packaged war project, you can simply prepare and deploy with:</p>
 *
 * <p>$ mvn heroku:prepare heroku:deploy</p>
 */
@Mojo(name = "deploy")
public class DeployMojo extends AbstractHerokuMojo {
	/**
	 * Git Staging Dir (should not be under target/)
	 */
	@Parameter(property = "heroku.stagingDirectory", defaultValue = "${project.basedir}/tmp-git-deployment-staging")
	File stagingDirectory;

	/**
   * The local path to the SSH key to be used.
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
	}
}
