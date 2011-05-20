package com.google.code.play.selenium.step;

import junit.framework.Assert;

import com.google.code.play.selenium.Step;

public class WaitForEqualsStep
    implements Step
{

    private StringSeleniumCommand innerCommand;

    private String expected;

    public WaitForEqualsStep( StringSeleniumCommand innerCommand, String expected )
    {
        this.innerCommand = innerCommand;
        this.expected = expected;
    }

    public void execute()
        throws Exception
    {
        String xexpected = MultiLineHelper.brToNewLine( expected );
        for ( int second = 0;; second++ )
        {
            if ( second >= 60 )
                Assert.fail( "timeout" );
            try
            {
                String innerCommandResult = innerCommand.getString();
                boolean seleniumEqualsResult = EqualsHelper.seleniumEquals( xexpected, innerCommandResult );
                if ( seleniumEqualsResult )
                    break;
            }
            catch ( Exception e )
            {
            }
            Thread.sleep( 1000 );
        }
    }

    public String toString()
    {
        String cmd = innerCommand.command.substring( "get".length() );

        StringBuffer buf = new StringBuffer();
        buf.append( "waitFor" ).append( cmd ).append( "('" );
        buf.append( innerCommand.param1 ).append( "', '" ).append( expected ).append( "')" );
        return buf.toString();
    }

}
