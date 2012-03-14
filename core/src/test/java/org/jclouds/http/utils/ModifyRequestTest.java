/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
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
package org.jclouds.http.utils;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.jclouds.http.HttpRequest;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.jclouds.util.Strings2;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Set;

import static org.jclouds.http.utils.ModifyRequest.parseQueryToMap;
import static org.testng.Assert.assertEquals;

/**
 * @author Adrian Cole
 */
@Test(groups = "unit")
public class ModifyRequestTest {

   public void testEndpoint() {
      HttpRequest request = HttpRequest.builder().method("GET").endpoint(URI.create("http://foo")).build();

      assertEquals(ModifyRequest.endpoint(request, URI.create("http://bar")), HttpRequest.builder().method("GET")
         .endpoint(URI.create("http://bar")).build());
   }

   public void testReplaceHeader() {
      HttpRequest request = HttpRequest.builder().method("GET").endpoint(URI.create("http://foo"))
         .headers(ImmutableMultimap.of("foo", "bar")).build();

      assertEquals(
         ModifyRequest.replaceHeader(request, "foo", "baz"),
         HttpRequest.builder().method("GET").endpoint(URI.create("http://foo"))
            .headers(ImmutableMultimap.of("foo", "baz")).build());
   }

   public void testRemoveHeader() {
      HttpRequest request = HttpRequest.builder().method("GET").endpoint(URI.create("http://foo"))
         .headers(ImmutableMultimap.of("foo", "bar")).build();

      assertEquals(ModifyRequest.removeHeader(request, "foo"),
         HttpRequest.builder().method("GET").endpoint(URI.create("http://foo")).build());
   }

   public void testReplaceHeaders() {
      HttpRequest request = HttpRequest.builder().method("GET").endpoint(URI.create("http://foo"))
         .headers(ImmutableMultimap.of("foo", "bar", "rabbit", "tree")).build();

      assertEquals(
         ModifyRequest.replaceHeaders(request,
            ImmutableMultimap.of("foo", "bar", "rabbit", "robot", "robert", "baz")),
         HttpRequest.builder().method("GET").endpoint(URI.create("http://foo"))
            .headers(ImmutableMultimap.of("foo", "bar", "rabbit", "robot", "robert", "baz")).build());
   }

   public void testPutHeadersAddsAnotherValue() {
      HttpRequest request = HttpRequest.builder().method("GET").endpoint(URI.create("http://foo"))
         .headers(ImmutableMultimap.of("foo", "bar")).build();

      assertEquals(
         ModifyRequest.putHeaders(request, ImmutableMultimap.of("foo", "baz")),
         HttpRequest.builder().method("GET").endpoint(URI.create("http://foo"))
            .headers(ImmutableMultimap.<String, String>builder().put("foo", "bar").put("foo", "baz").build())
            .build());
   }

   public void testPutFormParamsAddsAnotherValue() {
      HttpRequest request = HttpRequest.builder().method("GET").endpoint(URI.create("http://foo"))
         .payload(Payloads.newStringPayload("foo=bar")).build();
      Payload payload = Payloads.newStringPayload("foo=bar&foo=baz");
      payload.getContentMetadata().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      assertEquals(ModifyRequest.putFormParams(request, ImmutableMultimap.of("foo", "baz")), HttpRequest.builder()
         .method("GET").endpoint(URI.create("http://foo")).payload(payload).build());
   }

   public void testParseBase64InForm() {
      Multimap<String, String> expects = LinkedListMultimap.create();
      expects.put("Version", "2010-06-15");
      expects.put("Action", "ModifyInstanceAttribute");
      expects.put("Attribute", "userData");
      expects.put("Value", "dGVzdA==");
      expects.put("InstanceId", "1");
      assertEquals(
         parseQueryToMap("Version=2010-06-15&Action=ModifyInstanceAttribute&Attribute=userData&Value=dGVzdA%3D%3D&InstanceId=1"),
         expects);
   }

   @Test
   public void testParseQueryToMapSingleParam() {
      Multimap<String, String> parsedMap = parseQueryToMap("v=1.3");
      assert parsedMap.keySet().size() == 1 : "Expected 1 key, found: " + parsedMap.keySet().size();
      assert parsedMap.keySet().contains("v") : "Expected v to be a part of the keys";
      String valueForV = Iterables.getOnlyElement(parsedMap.get("v"));
      assert valueForV.equals("1.3") : "Expected the value for 'v' to be '1.3', found: " + valueForV;
   }

   @Test
   public void testParseQueryToMapMultiParam() {
      Multimap<String, String> parsedMap = parseQueryToMap("v=1.3&sig=123");
      assert parsedMap.keySet().size() == 2 : "Expected 2 keys, found: " + parsedMap.keySet().size();
      assert parsedMap.keySet().contains("v") : "Expected v to be a part of the keys";
      assert parsedMap.keySet().contains("sig") : "Expected sig to be a part of the keys";
      String valueForV = Iterables.getOnlyElement(parsedMap.get("v"));
      assert valueForV.equals("1.3") : "Expected the value for 'v' to be '1.3', found: " + valueForV;
      String valueForSig = Iterables.getOnlyElement(parsedMap.get("sig"));
      assert valueForSig.equals("123") : "Expected the value for 'v' to be '123', found: " + valueForSig;
   }

   @Test
   public void testParseQueryEncodedWithDefaultJavaEncoder() {
      String key = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCc903twxU2zcQnIJdXv61RwZNZW94uId9qz08fgsBJsCOnHNIC4+L9k" +
         "DOA2IHV9cUfEDBm1Be5TbpadWwSbS/05E+FARH2/MCO932UgcKUq5PGymS0249fLCBPci5zoLiG5vIym+1ij1hL/nHvkK99NIwe7io+Lmp" +
         "9OcF3PTsm3Rgh5T09cRHGX9horp0VoAVa9vKJx6C1/IEHVnG8p0YPPa1lmemvx5kNBEiyoNQNYa34EiFkcJfP6rqNgvY8h/j4nE9SXoUCC" +
         "/g6frhMFMOL0tzYqvz0Lczqm1Oh4RnSn3O9X4R934p28qqAobe337hmlLUdb6H5zuf+NwCh0HdZ";

      Set<String> expected = ImmutableSet.of(key);

      Multimap<String, String> parsedMap = parseQueryToMap("a=1&b=1+2&publickey=" + Strings2.urlEncode(key));
      assertEquals(parsedMap.get("publickey"), expected);

      parsedMap = parseQueryToMap("publickey=" + Strings2.urlEncode(key));
      assertEquals(parsedMap.get("publickey"), expected);
   }

   @Test
   public void testParseQueryWithKeysThatRequireDecoding() {
      Multimap<String, String> parsedMap = parseQueryToMap("network%5B0%5D.id=23&network%5B0%5D.address=192.168.0.1");

      assertEquals(parsedMap.get("network[0].id"), ImmutableSet.of("23"));
      assertEquals(parsedMap.get("network[0].address"), ImmutableSet.of("192.168.0.1"));
   }

}
