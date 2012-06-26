package com.google.code.play;

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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigurationParser
{

    private File directory;

    private File playHome;

    private String playId;

    private Properties properties;

    // private String applicationName;
    // private Map<String, File> modules;

    public ConfigurationParser( File directory, File playHome, String playId )
    {
        this.directory = directory;
        this.playHome = playHome;
        this.playId = playId;
        // modules = new HashMap<String, File>();
    }

    public String getProperty( String key )
    {
        Object value = null;
        if ( playId != null && !"".equals( playId ) )
        {
            value = properties.get( "%" + playId + "." + key );
        }
        if ( value == null )
        {
            value = properties.get( key );
        }
        return (String) value;
    }

    public String getApplicationName()
    {
        // return applicationName;
        return getProperty( "application.name" );
    }

    public Map<String, File> getModules()
    {
        Map<String, File> modules = new HashMap<String, File>();
        for ( Object key : properties.keySet() )
        {
            String strKey = (String) key;
            if ( strKey.startsWith( "module." ) )
            {
                String moduleName = strKey.substring( 7 );
                String modulePath = (String) properties.get( key );
                modulePath = modulePath.replace( "${play.path}", playHome.getPath() );
                modules.put( moduleName, new File( modulePath ) );
            }
        }
        // optimize?
        for ( Object key : properties.keySet() )
        {
            String strKey = (String) key;
            if ( strKey.startsWith( "%" + playId + ".module." ) )
            {
                String moduleName = strKey.substring( 7 + playId.length() + 2 );
                String modulePath = (String) properties.get( key );
                modulePath = modulePath.replace( "${play.path}", playHome.getPath() );
                modules.put( moduleName, new File( modulePath ) );
            }
        }
        return modules;
    }

    public void parse()
        throws IOException/* , JSONException */
    {
        File configFile = new File( directory, "application.conf" );
        InputStream inputStream = new BufferedInputStream( new FileInputStream( configFile ) );
        try
        {
            Properties props = new Properties();
            props.load( inputStream );
            // applicationName = (String)props.get("application.name");
            /*
             * for (Object key: props.keySet()) { String strKey = (String)key; if (strKey.startsWith("module.")) {
             * String moduleName = strKey.substring(7); String modulePath = (String)props.get(key); modulePath =
             * modulePath.replace("${play.path}", playHome.getCanonicalPath()); modules.put(moduleName, new
             * File(modulePath)); } }
             */
            this.properties = props;
            // result = provides.getString("namespace");
        }
        finally
        {
            inputStream.close();
        }
    }

}