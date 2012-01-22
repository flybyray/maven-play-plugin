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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
//import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

/**
 * Start Play! Server ("play start" equivalent).
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @goal start
 * @requiresDependencyResolution test
 */
public class PlayStartMojo
    extends AbstractPlayServerMojo
{
    /**
     * Play! id (profile) used for testing.
     * 
     * @parameter expression="${play.id}" default-value=""
     * @since 1.0.0
     */
    protected String playId;

//    /**
//     * Arbitrary JVM options to set on the command line.
//     * 
//     * @parameter expression="${play.serverProcessArgLine}"
//     * @since 1.0.0
//     */
//    private String serverProcessArgLine;

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        File playHome = getPlayHome();
        File baseDir = project.getBasedir();

        File pidFile = new File( baseDir, "server.pid" );
        if ( pidFile.exists() )
        {
            throw new MojoExecutionException( String.format( "Play! Server already started (\"%s\" file found)",
                                                             pidFile.getName() ) );
        }

        File confDir = new File( baseDir, "conf" );
        File configurationFile = new File( confDir, "application.conf" );

        ConfigurationParser configParser = new ConfigurationParser( configurationFile, playId );
        configParser.parse();

        String sysOut = configParser.getProperty( "application.log.system.out" );
        boolean redirectSysOutToFile = !( "false".equals( sysOut ) || "off".equals( sysOut ) );

        File logDirectory = new File( baseDir, "logs" );
        File logFile = new File(logDirectory, "system.out");
        if (redirectSysOutToFile) {
            if ( !logDirectory.exists() && !logDirectory.mkdirs() )
            {
                throw new MojoExecutionException( String.format( "Cannot create %s directory", logDirectory.getAbsolutePath() ) );
            }
        }

        Project antProject = createProject();
        Path classPath = getProjectClassPath(antProject, playId);

        Java javaTask = new Java();
        javaTask.setProject( antProject );
        javaTask.setClassname( "com.google.code.play.PlayServerBooter" );
        javaTask.setFork( true );
        javaTask.setSpawn( true );
        javaTask.setDir( baseDir );
        javaTask.setClasspath( classPath );
        addSystemProperty( javaTask, "play.home", playHome.getAbsolutePath() );
        addSystemProperty( javaTask, "play.id", ( playId != null ? playId : "" ) );
        addSystemProperty( javaTask, "application.path", baseDir.getAbsolutePath() );
        //because of Java limitations:
        //- cannot manipulate input/output streams of spawned process
        //- cannot get process id of spawned process
        addSystemProperty( javaTask, "pidFile", pidFile.getAbsolutePath()/*"server.pid"*/ );
        if ( redirectSysOutToFile )
        {
            addSystemProperty( javaTask, "outFile", logFile.getAbsolutePath()/*"logs/system.out"*/ );
        }

// Not ready yet
//        if ( serverProcessArgLine != null )//TODO-what should be default value??? why it does not work???
//        {
//            String argLine = serverProcessArgLine.trim();
//            if ( !"".equals( argLine ) )
//            {
//                String[] args = argLine.split( " " );
//                for ( String arg : args )
//                {
//                    Commandline.Argument jvmArg = javaTask.createJvmarg();
//                    jvmArg.setValue( arg );
//                    getLog().debug( "  Adding jvmarg '" + arg + "'" );
//                }
//            }
//        }

        JavaRunnable runner = new JavaRunnable( javaTask );
        Thread t = new Thread( runner, "Play! Server runner" );
        t.start();
        try
        {
            t.join();
        }
        catch ( InterruptedException e )
        {
            throw new MojoExecutionException( "?", e );
        }
        Exception startServerException = runner.getException();
        if ( startServerException != null )
        {
            throw new MojoExecutionException( "?", startServerException );
        }
        
        if ( redirectSysOutToFile )
        {
            getLog().info( String.format( "Play! Server started, output is redirected to %s", logFile.getPath() ) );
        }
        else
        {
            getLog().info( "Play! Server started" );
        }
    }

}
