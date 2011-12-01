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

/**
 * Prepares project for WAR packaging.
 * For now only prepares "web.xml" file (replaces %APPLICATION_NAME% and %PLAY_ID% with actual values).
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @goal war-support
 * @phase prepare-package
 */
public class PlayWarSupportMojo
    extends AbstractPlayMojo
{
    /**
     * Play! id (profile) used for WAR packaging.
     * 
     * @parameter expression="${play.warId}" default-value="war"
     * @since 1.0.0
     */
    protected String playWarId;

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        File playHome = getPlayHome();

        File baseDir = project.getBasedir();
        File confDir = new File( baseDir, "conf" );
        File configurationFile = new File( confDir, "application.conf" );

        ConfigurationParser configParser = new ConfigurationParser( configurationFile, playWarId );
        configParser.parse();
        // Map<String, File> modules = configParser.getModules();

        /*
         * File filteredApplicationConf =
         */// filterApplicationConf( new File( baseDir, "conf/application.conf" ), modules );

        File buildDirectory = new File( project.getBuild().getDirectory() );
        File tmpDirectory = new File( buildDirectory, "play/tmp" );
        /* File filteredWebXml = */filterWebXml( new File( playHome, "resources/war/web.xml" ), tmpDirectory,
                                                 configParser.getApplicationName(), playWarId );
    }

}
