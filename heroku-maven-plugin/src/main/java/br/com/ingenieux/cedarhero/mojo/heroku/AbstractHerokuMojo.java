package br.com.ingenieux.cedarhero.mojo.heroku;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

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

/**
 * The Basic Heroku Mojo
 *
 * Parts of this class come from <a
 * href="http://code.google.com/p/maven-gae-plugin">maven-gae-plugin</a>'s
 * source code.
 */
public abstract class AbstractHerokuMojo extends AbstractMojo {
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      executeInternal();
    } catch (Exception exc) {
      if (MojoExecutionException.class.isAssignableFrom(exc.getClass())) {
        throw (MojoExecutionException) exc;
      } else if (MojoFailureException.class.isAssignableFrom(exc.getClass())) {
        throw (MojoFailureException) exc;
      } else {
        throw new RuntimeException(exc);
      }
    }
  }

  protected abstract void executeInternal() throws Exception;

  protected void log(Throwable e, String message, Object... args) {
    getLog().info(String.format(message, args), e);
  }

  protected void log(String message, Object... args) {
    getLog().info(String.format(message, args));
  }
}
