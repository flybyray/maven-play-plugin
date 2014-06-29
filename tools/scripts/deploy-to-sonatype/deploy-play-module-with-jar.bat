set GROUP_ID_PREFIX=com.google.code.maven-play-plugin.
set GROUP_ID=%GROUP_ID_PREFIX%org.playframework.modules.%MODULE_NAME%
set ARTIFACT_ID=play-%MODULE_NAME%
set SRC_DIR=%PLAY_HOME%/modules/%MODULE_NAME%

set REPO_ID=sonatype-nexus-staging
set REPO_URL=https://oss.sonatype.org/service/local/staging/deploy/maven2

call mvn clean package source:jar javadoc:jar -Pdist --file %SRC_DIR%/pom-build-dist.xml

call mvn gpg:sign-and-deploy-file -Durl=%REPO_URL% -DrepositoryId=%REPO_ID% -DpomFile=%SRC_DIR%/pom-dist.xml -Dfile=%SRC_DIR%/lib/%ARTIFACT_ID%.jar -Dclassifiers=sources,javadoc,module-min,module -Dtypes=jar,jar,zip,zip -Dfiles=%SRC_DIR%/target/%ARTIFACT_ID%-%VERSION%-sources.jar,%SRC_DIR%/target/%ARTIFACT_ID%-%VERSION%-javadoc.jar,%SRC_DIR%/target/%ARTIFACT_ID%-%VERSION%-module-min.zip,%SRC_DIR%/target/%ARTIFACT_ID%-%VERSION%-module.zip
