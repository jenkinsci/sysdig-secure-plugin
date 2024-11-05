
update: update-parent-pom update-dependencies

update-parent-pom:
	mvn versions:update-parent

update-dependencies:
	mvn versions:use-latest-versions

verify:
	mvn clean verify javadoc:jar
