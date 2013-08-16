# cedarhero faq

## How does cedarhero works?

cedarhero works by:

  * creating a directory (by default, called tmp-git-staging) under your base directory
  * git cloning a base appserver repository (by default, [cedarhero-jetty](https://bitbucket.org/ingenieux/cedarhero-jetty))
  * copying your webapp contents into /webapp
  * setting the remote uri

## Which cases makes cedarhero more suitable?

A Couple. Specially:

  * When your sources are not held in git (e.g. here, we prefer mercurial)
  * You don't want to hack too much your stuff to deploy into heroku, like:
    * General pom.xml hackery
    * settings.xml
    * Extra, firewalled repository servers
  * You want minimal to none tooling installed (it uses jGit, needs no Ruby, out of the box without extra dependencies installed)
  * You want to split application server setup from artifact (and avoiding slug compilation

## I want to create a custom appserver replacement...

No problem. Clone [cedarhero-jetty](https://bitbucket.org/ingenieux/cedarhero-jetty) and replace your ```sourceStackRepository``` variable in your pom.xml, like this one:

```
<build>
  <plugins>
    <plugin>
      <groupId>br.com.ingenieux.cedarhero</groupId>
      <artifactId>heroku-maven-plugin</artifactId>
      <version>${project.version}</version>
      <configuration>
        <app>lovely-app-9999</app>
        <sourceStackRepository>https://bitbucket.org/ingenieux/cedarhero-winstone.git</sourceStackRepository>
      </configuration>
    </plugin>
  </plugins>
</build>
```
There might be a few limitations on how cloning the sourceStack works. By default, prefer https urls specially on bitbucket.

## Can you do... x?

Sure, we can. File a issue request. We created it most as a proof of concept, but we're willing to extend if users show demand (specially on [issues](http://github.com/ingenieux/cedarhero/issues) and the [http://groups.google.com/group/cedarhero-users/](mailing list)