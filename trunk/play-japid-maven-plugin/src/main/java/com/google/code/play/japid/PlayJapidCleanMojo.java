/*
 * Copyright 2012-2013 Grzegorz Slowikowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.play.japid;

import org.apache.maven.plugins.annotations.Mojo;

/**
 * Deleting Java source files generated from Japid templates.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 0.9.3.4
 */
@Mojo( name = "clean" )
public class PlayJapidCleanMojo
    extends AbstractPlayJapidMojo
{
    protected String getCommand()
    {
        return "clean";
    }

}
