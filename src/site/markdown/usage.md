# cedarhero usage

## pom setup

Thats all you need to add to your pom.xml:

```
<build>
  <plugins>
    <plugin>
      <groupId>br.com.ingenieux.cedarhero</groupId>
      <artifactId>heroku-maven-plugin</artifactId>
      <version>${project.version}</version>
      <configuration>
        <app>lovely-app-9999</app>
      </configuration>
    </plugin>
  </plugins>
</build>
```

## Invoking

Once set, just add two new commands to your build command:

```
$mvn package heroku:prepare heroku:deploy
```

Questions? Please look (the faq)[./faq.html]