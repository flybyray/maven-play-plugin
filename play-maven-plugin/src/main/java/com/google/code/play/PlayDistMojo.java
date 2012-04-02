/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.google.code.play;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

/**
 * Packages Play! framework and Play! application as one ZIP achive (standalone distribution).
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @goal dist
 * @phase package
 * @requiresDependencyResolution test
 */
public class PlayDistMojo
    extends AbstractDependencyProcessingPlayMojo
{

    /**
     * Default Play! id (profile).
     * 
     * @parameter expression="${play.id}" default-value=""
     * @since 1.0.0
     */
    protected String playId;

    /**
     * Skip distribution file generation.
     * 
     * @parameter expression="${play.distSkip}" default-value="false"
     * @required
     * @since 1.0.0
     */
    private boolean distSkip = false;

    /**
     * The directory for the generated distribution file.
     * 
     * @parameter expression="${play.distOutputDirectory}" default-value="${project.build.directory}"
     * @required
     * @since 1.0.0
     */
    private String distOutputDirectory;

    /**
     * The name of the generated distribution file.
     * 
     * @parameter expression="${play.distArchiveName}" default-value="${project.build.finalName}"
     * @required
     * @since 1.0.0
     */
    private String distArchiveName;

    /**
     * Classifier to add to the generated distribution file.
     * 
     * @parameter expression="${play.distClassifier}" default-value="dist"
     * @since 1.0.0
     */
    private String distClassifier;

    /**
     * Attach generated distribution file to project artifacts.
     * 
     * @parameter expression="${play.distAttach}" default-value="false"
     * @since 1.0.0
     */
    private boolean distAttach;

    /**
     * Distribution application resources include filter
     * 
     * @parameter expression="${play.distApplicationIncludes}" default-value="app/**,conf/**,public/**,tags/**,test/**"
     * @since 1.0.0
     */
    private String distApplicationIncludes;

    /**
     * Distribution application resources exclude filter.
     * 
     * @parameter expression="${play.distApplicationExcludes}" default-value=""
     * @since 1.0.0
     */
    private String distApplicationExcludes;

    /**
     * Distribution dependency include filter.
     * 
     * @parameter expression="${play.distDependencyIncludes}" default-value=""
     * @since 1.0.0
     */
    private String distDependencyIncludes;

    /**
     * Distribution dependency exclude filter.
     * 
     * @parameter expression="${play.distDependencyExcludes}" default-value=""
     * @since 1.0.0
     */
    private String distDependencyExcludes;

    /**
     * To look up Archiver/UnArchiver implementations.
     * 
     * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
     * @required
     */
    private ArchiverManager archiverManager;

    /**
     * Maven ProjectHelper.
     * 
     * @component
     */
    private MavenProjectHelper projectHelper;

    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        if ( distSkip )
        {
            getLog().info( "UberZip generation skipped" );
            return;
        }

        try
        {
            File baseDir = project.getBasedir();
            File destFile = new File( distOutputDirectory, getDestinationFileName() );

            File confDir = new File( baseDir, "conf" );
            File configurationFile = new File( confDir, "application.conf" );
            ConfigurationParser configParser = new ConfigurationParser( configurationFile, playId );
            configParser.parse();
            Set<String> providedModuleNames = getProvidedModuleNames( configParser, playId, false );

            Archiver zipArchiver = archiverManager.getArchiver( "zip" );
            zipArchiver.setDuplicateBehavior( Archiver.DUPLICATES_FAIL ); // Just in case
            zipArchiver.setDestFile( destFile );

            // APPLICATION
            getLog().debug( "UberZip includes: " + distApplicationIncludes );
            getLog().debug( "UberZip excludes: " + distApplicationExcludes );
            String[] applicationIncludes = null;
            if ( distApplicationIncludes != null )
            {
                applicationIncludes = distApplicationIncludes.split( "," );
            }
            // TODO-don't add "test/**" if profile is not test profile
            String[] applicationExcludes = null;
            if ( distApplicationExcludes != null )
            {
                applicationExcludes = distApplicationExcludes.split( "," );
            }
            zipArchiver.addDirectory( baseDir, "application/", applicationIncludes, applicationExcludes );

            // preparation
            Set<?> projectArtifacts = project.getArtifacts();

            Set<Artifact> excludedArtifacts = new HashSet<Artifact>();
            Artifact playSeleniumJunit4Artifact =
                getDependencyArtifact( projectArtifacts, "com.google.code.maven-play-plugin", "play-selenium-junit4",
                                       "jar" );
            if ( playSeleniumJunit4Artifact != null )
            {
                excludedArtifacts.addAll( getDependencyArtifacts( projectArtifacts, playSeleniumJunit4Artifact ) );
            }

            AndArtifactFilter dependencyFilter = new AndArtifactFilter();
            if ( distDependencyIncludes != null && !distDependencyIncludes.isEmpty() )
            {
                List<String> incl = Arrays.asList( distDependencyIncludes.split( "," ) ); 
                PatternIncludesArtifactFilter includeFilter =
                    new PatternIncludesArtifactFilter( incl, true/* actTransitively */ );

                dependencyFilter.add( includeFilter );
            }
            if ( distDependencyExcludes != null && !distDependencyExcludes.isEmpty() )
            {
                List<String> excl = Arrays.asList( distDependencyExcludes.split( "," ) ); 
                PatternExcludesArtifactFilter excludeFilter =
                    new PatternExcludesArtifactFilter( excl, true/* actTransitively */ );

                dependencyFilter.add( excludeFilter );
            }

            Set<Artifact> filteredArtifacts = new HashSet<Artifact>(); // TODO-rename to filteredClassPathArtifacts
            for ( Iterator<?> iter = projectArtifacts.iterator(); iter.hasNext(); )
            {
                Artifact artifact = (Artifact) iter.next();
                if ( artifact.getArtifactHandler().isAddedToClasspath() && !excludedArtifacts.contains( artifact ) )
                {
                    // TODO-add checkPotentialReactorProblem( artifact );
                    if ( dependencyFilter.include( artifact ) )
                    {
                        filteredArtifacts.add( artifact );
                    }
                    else
                    {
                        getLog().debug( artifact.toString() + " excluded" );
                    }
                }
            }

            // framework
            Artifact frameworkZipArtifact = findFrameworkArtifact( false );
            File frameworkZipFile = frameworkZipArtifact.getFile();
            zipArchiver.addArchivedFileSet( frameworkZipFile );
            Artifact frameworkJarArtifact =
                getDependencyArtifact( filteredArtifacts/* ?? */, frameworkZipArtifact.getGroupId(),
                                       frameworkZipArtifact.getArtifactId(), "jar" );
            // TODO-validate not null
            File frameworkJarFile = frameworkJarArtifact.getFile();
            String frameworkDestinationFileName = "framework/" + frameworkJarFile.getName();
            String playVersion = frameworkJarArtifact.getVersion();
            if ( "1.2".compareTo( playVersion ) > 0 )
            {
                // Play 1.1.x
                frameworkDestinationFileName = "framework/play.jar";
            }
            zipArchiver.addFile( frameworkJarFile, frameworkDestinationFileName );
            filteredArtifacts.remove( frameworkJarArtifact );
            Set<Artifact> dependencySubtree = getDependencyArtifacts( filteredArtifacts/* ?? */, frameworkJarArtifact );
            for ( Artifact classPathArtifact : dependencySubtree )
            {
                File jarFile = classPathArtifact.getFile();
                String destinationFileName = "framework/lib/" + jarFile.getName();
                zipArchiver.addFile( jarFile, destinationFileName );
                filteredArtifacts.remove( classPathArtifact );
            }

            // modules/*/lib and application/modules/*/lib
            Set<Artifact> notActiveProvidedModules = new HashSet<Artifact>();
            Map<String, Artifact> moduleArtifacts = findAllModuleArtifacts( false );
            for ( Map.Entry<String, Artifact> moduleArtifactEntry : moduleArtifacts.entrySet() )
            {
                String moduleName = moduleArtifactEntry.getKey();
                Artifact moduleZipArtifact = moduleArtifactEntry.getValue();

                File moduleZipFile = moduleZipArtifact.getFile();
                if ( Artifact.SCOPE_PROVIDED.equals( moduleZipArtifact.getScope() ) )
                {
                    if ( providedModuleNames.contains( moduleName ) )
                    {
                        String moduleSubDir =
                            String.format( "modules/%s/", moduleName/* , moduleArtifact.getVersion() */ );
                        zipArchiver.addArchivedFileSet( moduleZipFile, moduleSubDir );
                        dependencySubtree = getModuleDependencyArtifacts( filteredArtifacts, moduleZipArtifact );
                        for ( Artifact classPathArtifact : dependencySubtree )
                        {
                            File jarFile = classPathArtifact.getFile();
                            String destinationFileName = jarFile.getName();
                            // Scala module hack
                            if ( "scala".equals( moduleName ) )
                            {
                                destinationFileName = scalaHack( classPathArtifact );
                            }
                            String destinationPath =
                                String.format( "modules/%s/lib/%s", moduleName, destinationFileName );
                            zipArchiver.addFile( jarFile, destinationPath );
                            filteredArtifacts.remove( classPathArtifact );
                        }
                    }
                    else
                    {
                        notActiveProvidedModules.add( moduleZipArtifact );
                    }
                }
                else
                {
                    String moduleSubDir =
                        String.format( "application/modules/%s-%s/", moduleName, moduleZipArtifact.getVersion() );
                    zipArchiver.addArchivedFileSet( moduleZipFile, moduleSubDir );
                    dependencySubtree = getModuleDependencyArtifacts( filteredArtifacts, moduleZipArtifact );
                    for ( Artifact classPathArtifact : dependencySubtree )
                    {
                        File jarFile = classPathArtifact.getFile();
                        String destinationFileName = jarFile.getName();
                        // Scala module hack
                        if ( "scala".equals( moduleName ) )
                        {
                            destinationFileName = scalaHack( classPathArtifact );
                        }
                        String destinationPath =
                            String.format( "application/modules/%s-%s/lib/%s", moduleName,
                                           moduleZipArtifact.getVersion(), destinationFileName );
                        zipArchiver.addFile( jarFile, destinationPath );
                        filteredArtifacts.remove( classPathArtifact );
                    }
                }
            }

            for ( Artifact moduleZipArtifact : notActiveProvidedModules )
            {
                dependencySubtree = getModuleDependencyArtifacts( filteredArtifacts, moduleZipArtifact );
                filteredArtifacts.removeAll( dependencySubtree );
            }

            // application/lib
            for ( Iterator<?> iter = filteredArtifacts.iterator(); iter.hasNext(); )
            {
                Artifact artifact = (Artifact) iter.next();
                File jarFile = artifact.getFile();
                String destinationFileName = "application/lib/" + jarFile.getName();
                zipArchiver.addFile( jarFile, destinationFileName );
            }

            zipArchiver.createArchive();
            
            if ( distAttach )
            {
                projectHelper.attachArtifact( project, "zip", distClassifier, destFile );
            }
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "?", e );
        }
        catch ( DependencyTreeBuilderException e )
        {
            throw new MojoExecutionException( "?", e );
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "?", e );
        }
    }

    private String getDestinationFileName()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( distArchiveName );
        if ( distClassifier != null && !"".equals( distClassifier ) )
        {
            if ( !distClassifier.startsWith( "-" ) )
            {
                buf.append( '-' );
            }
            buf.append( distClassifier );
        }
        buf.append( ".zip" );
        return buf.toString();
    }

    private String scalaHack( Artifact dependencyArtifact ) throws IOException
    {
        String destinationFileName = dependencyArtifact.getFile().getName();
        if ( "org.scala-lang".equals( dependencyArtifact.getGroupId() )
            && ( "scala-compiler".equals( dependencyArtifact.getArtifactId() ) || "scala-library".equals( dependencyArtifact.getArtifactId() ) )
            && "jar".equals( dependencyArtifact.getType() ) )
        {
            destinationFileName = dependencyArtifact.getArtifactId() + ".jar";
        }
        return destinationFileName;
    }

}
