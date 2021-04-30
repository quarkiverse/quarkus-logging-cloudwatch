/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkiverse.logging.cloudwatch.it;

import com.amazonaws.services.s3.AmazonS3;

//@QuarkusTest
//@QuarkusTestResource(LoggingCloudwatchResource.class)
public class LoggingCloudwatchHandlerResourceTest {

    static AmazonS3 S3;

    //    @Test
    //    public void testHelloEndpoint() {
    //        given()
    //                .when().get("/logging-cloudwatch")
    //                .then()
    //                .statusCode(200)
    //                .body(is("Hello logging-cloudwatch"));
    //    }

    //    @Test
    //    public void someTestMethod() {
    //        AmazonS3 s3 = S3;
    //
    //        s3.createBucket("foo");
    //        s3.putObject("foo", "bar", "baz");
    //        final S3Object s3Object = s3.getObject("foo", "bar");
    //
    //        assertEquals("foo", s3Object.getBucketName());
    //        assertEquals("bar", s3Object.getKey());
    //    }
}
