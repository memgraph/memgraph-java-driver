/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package neo4j.org.testkit.backend.messages.requests;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import neo4j.org.testkit.backend.TestkitState;
import neo4j.org.testkit.backend.messages.responses.NullRecord;
import neo4j.org.testkit.backend.messages.responses.Summary;
import neo4j.org.testkit.backend.messages.responses.TestkitResponse;

import org.neo4j.driver.Result;
import org.neo4j.driver.exceptions.NoSuchRecordException;

@Setter
@Getter
@NoArgsConstructor
public class ResultConsume implements TestkitRequest
{
    private ResultConsumeBody data;

    @Override
    public TestkitResponse process( TestkitState testkitState )
    {
        try
        {
            Result result = testkitState.getResults().get( data.getResultId() );
            org.neo4j.driver.summary.ResultSummary summary = result.consume();
            Summary.ServerInfo serverInfo = Summary.ServerInfo.builder()
                                                              .protocolVersion( summary.server().protocolVersion() )
                                                              .agent( summary.server().agent() )
                                                              .build();
            Summary.SummaryBody data = Summary.SummaryBody.builder()
                                                          .serverInfo( serverInfo )
                                                          .build();
            return Summary.builder()
                          .data( data )
                          .build();
        }
        catch ( NoSuchRecordException ignored )
        {
            return NullRecord.builder().build();
        }
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class ResultConsumeBody
    {
        private String resultId;
    }
}
