package com.google.code.play.surefire.junit4;

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

import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.common.junit4.JUnit4RunListener;
import org.apache.maven.surefire.common.junit4.JUnit4RunListenerFactory;
import org.apache.maven.surefire.common.junit4.JUnit4TestChecker;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.PojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.Reporter;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.DirectoryScanner;
import org.apache.maven.surefire.util.TestsToRun;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

import play.Play;
import play.PlayPlugin;
import play.vfs.VirtualFile;
//import org.apache.maven.surefire.util.DefaultDirectoryScanner;
//import org.apache.maven.surefire.util.DirectoryScanner;
//import org.apache.maven.surefire.util.TestsToRun;
//import org.junit.runner.notification.RunListener;
//import org.junit.runner.notification.RunNotifier;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

//@SuppressWarnings( { "UnusedDeclaration" } )
public class PlayJUnit4Provider
    implements SurefireProvider
{

    private static ResourceBundle bundle = ResourceBundle.getBundle( Surefire.SUREFIRE_BUNDLE_NAME );

    private final ReporterFactory reporterFactory;

    private final ClassLoader testClassLoader;

    private final DirectoryScanner directoryScanner;
    
    private final Properties providerProperties; 

    private final List<RunListener> customRunListeners;

    private final JUnit4TestChecker jUnit4TestChecker;

    private TestsToRun testsToRun;
    
    private final boolean skipPlay;

    public PlayJUnit4Provider( ProviderParameters booterParameters )
    {
        this.reporterFactory = booterParameters.getReporterFactory();
        this.testClassLoader = booterParameters.getTestClassLoader();
        this.directoryScanner = booterParameters.getDirectoryScanner();
        this.providerProperties = booterParameters.getProviderProperties();
        this.skipPlay = "true".equals(providerProperties.getProperty( "skipPlay" ));
        customRunListeners =
            JUnit4RunListenerFactory.createCustomListeners( booterParameters.getProviderProperties().getProperty( "listener" ) );
        jUnit4TestChecker = new JUnit4TestChecker( testClassLoader );

    }

    // @SuppressWarnings( { "UnnecessaryUnboxing" } )
    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException
    {
        if (!skipPlay) {
            System.out.println( "Play! initialization" );
            initializePlayEngine();// tu?
        }
        try
        {
            if ( testsToRun == null )
            {
                testsToRun = forkTestSet == null ? scanClassPath() : TestsToRun.fromClass( (Class) forkTestSet );
            }

            Reporter reporter = reporterFactory.createReporter();
            JUnit4RunListener jUnit4TestSetReporter = new JUnit4RunListener( reporter );
            RunNotifier runNotifer = getRunNotifer( jUnit4TestSetReporter, customRunListeners );

            for ( Class clazz : testsToRun.getLocatedClasses() )
            {
                executeTestSet( clazz, reporter, testClassLoader, runNotifer );
            }

            closeRunNotifer( jUnit4TestSetReporter, customRunListeners );

            return reporterFactory.close();
        }
        finally
        {
            if (!skipPlay) {
                System.out.println( "Play! finalization" );
                finalizePlayEngine();// tu?
            }
        }
    }

    private void initializePlayEngine()
    {
        File playHome = new File( System.getProperty( "play.home" ) );
        File applicationPath = new File( System.getProperty( "application.path" ) );

        // System.out.println("play.id='"+System.getProperty("play.id")+"'");
        Play.frameworkPath = playHome;
        Play.init( applicationPath, "test"/* ??? playId */);

        Play.start();
    }

    private void finalizePlayEngine()
    {
        Play.stop();
    }

    private void executeTestSet( Class clazz, Reporter reporter, ClassLoader classLoader, RunNotifier listeners )
        throws ReporterException, TestSetFailedException
    {
        final ReportEntry report = new SimpleReportEntry( this.getClass().getName(), clazz.getName() );

        reporter.testSetStarting( report );

        try
        {
            PlayJUnit4TestSet.execute( clazz, listeners );
        }
        catch ( TestSetFailedException e )
        {
            throw e;
        }
        catch ( Throwable e )
        {
            reporter.testError( new SimpleReportEntry( report.getSourceName(), report.getName(),
                                                       new PojoStackTraceWriter( report.getSourceName(),
                                                                                 report.getName(), e ) ) );
        }
        finally
        {
            reporter.testSetCompleted( report );
        }
    }

    private RunNotifier getRunNotifer( RunListener main, List<RunListener> others )
    {
        RunNotifier fNotifier = new RunNotifier();
        fNotifier.addListener( main );
        for ( RunListener listener : others )
        {
            fNotifier.addListener( listener );
        }
        return fNotifier;
    }

    // I am not entierly sure as to why we do this explicit freeing, it's one of those
    // pieces of code that just seem to linger on in here ;)
    private void closeRunNotifer( RunListener main, List<RunListener> others )
    {
        RunNotifier fNotifier = new RunNotifier();
        fNotifier.removeListener( main );
        for ( RunListener listener : others )
        {
            fNotifier.removeListener( listener );
        }
    }

    public Iterator getSuites()
    {
        testsToRun = scanClassPath();
        return testsToRun.iterator();
    }

    private TestsToRun scanClassPath()
    {
        ClassLoader classLoader = (this.skipPlay ? testClassLoader : Play.classloader);
        return directoryScanner.locateTestClasses( classLoader/*test Play.classloader*//* testClassLoader */, jUnit4TestChecker );
    }

    public Boolean isRunnable()
    {
        return Boolean.TRUE;
    }
}
