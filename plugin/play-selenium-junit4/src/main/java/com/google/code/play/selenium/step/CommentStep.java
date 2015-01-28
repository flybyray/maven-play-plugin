/*
 * Copyright 2010-2015 Grzegorz Slowikowski
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

package com.google.code.play.selenium.step;

import com.google.code.play.selenium.Step;

public class CommentStep
    implements Step
{

    private String comment;

    public CommentStep( String comment )
    {
        this.comment = comment;
    }

    public void execute()
        throws Exception
    {
    }

    public long getExecutionTimeMillis()
    {
        return -1L;
    }

    public String toString()
    {
        return comment;
    }
}
